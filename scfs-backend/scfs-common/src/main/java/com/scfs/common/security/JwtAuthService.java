package com.scfs.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.constant.ScfsConstants;
import com.scfs.common.entity.SysUser;
import com.scfs.common.mapper.SysMenuMapper;
import com.scfs.common.mapper.SysUserMapper;
import com.scfs.common.service.SysUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 认证服务 - 对应 RFC 4.1.1 AuthService
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li>Access Token 2 小时，Refresh Token 7 天</li>
 *   <li>Refresh Token 存 Redis，登出时删除</li>
 *   <li>双 Token 机制，Access Token 过期前端使用 Refresh Token 换新</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthService {

    private final SysUserService userService;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityContextHelper securityContextHelper;

    @Value("${scfs.security.jwt.secret}")
    private String secret;

    @Value("${scfs.security.jwt.access-token-expire:PT2H}")
    private Duration accessTokenExpire;

    @Value("${scfs.security.jwt.refresh-token-expire:P7D}")
    private Duration refreshTokenExpire;

    @Value("${scfs.security.jwt.issuer:scfs}")
    private String issuer;

    /**
     * 登录：返回双 Token
     */
    public Map<String, Object> login(String username, String rawPassword) {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new IllegalStateException("用户已被禁用");
        }
        if (!userService.checkPassword(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        // Refresh Token 存 Redis（用于校验有效性）
        String redisKey = ScfsConstants.CACHE_REFRESH_TOKEN + refreshToken;
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), refreshTokenExpire);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", accessTokenExpire.getSeconds());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleCode", user.getRoleCode());
        // 查询并填充 permissions（module -> [action,...]）
        Map<String, List<String>> permissions = loadPermissions(user.getId());
        userInfo.put("permissions", permissions);
        // 同步写入 Redis 缓存，供 PermissionCheckerAspect 后续校验使用
        securityContextHelper.cacheRolePermissions(user.getRoleCode(), permissions);
        userInfo.put("menuCodes", menuMapper.selectMenuCodesByRoleCode(user.getRoleCode()));
        result.put("userInfo", userInfo);

        return result;
    }

    /** Return the current user's latest role and permissions. */
    public Map<String, Object> currentUserInfo() {
        Long userId = securityContextHelper.getCurrentUserIdOrThrow();
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalStateException("用户不存在或已被禁用");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleCode", user.getRoleCode());
        Map<String, List<String>> permissions = loadPermissions(user.getId());
        userInfo.put("permissions", permissions);
        securityContextHelper.cacheRolePermissions(user.getRoleCode(), permissions);
        userInfo.put("menuCodes", menuMapper.selectMenuCodesByRoleCode(user.getRoleCode()));
        return userInfo;
    }

    /**
     * 加载用户角色的权限映射：module -> permissions[]
     */
    private Map<String, List<String>> loadPermissions(Long userId) {
        List<Map<String, Object>> rows = userMapper.selectPermissionsByUserId(userId);
        Map<String, List<String>> permissions = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String module = (String) row.get("module");
                Object perms = row.get("permissions");
                if (module != null && perms instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> permList = (List<String>) perms;
                    permissions.put(module, permList);
                }
            }
        }
        return permissions;
    }

    /**
     * Refresh Token 换 Access Token
     */
    public Map<String, Object> refresh(String refreshToken) {
        String redisKey = ScfsConstants.CACHE_REFRESH_TOKEN + refreshToken;
        String userIdStr = redisTemplate.opsForValue().get(redisKey);
        if (userIdStr == null) {
            throw new IllegalStateException("Refresh Token 无效或已过期");
        }

        Long userId = Long.parseLong(userIdStr);
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            redisTemplate.delete(redisKey);
            throw new IllegalStateException("用户不存在或已被禁用");
        }

        String newAccessToken = generateAccessToken(user);
        // 滚动续期 Refresh Token
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), refreshTokenExpire);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", accessTokenExpire.getSeconds());
        return result;
    }

    /**
     * 登出：删除 Refresh Token
     */
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String redisKey = ScfsConstants.CACHE_REFRESH_TOKEN + refreshToken;
            redisTemplate.delete(redisKey);
        }
    }

    private String generateAccessToken(SysUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("roleCode", user.getRoleCode())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenExpire)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateRefreshToken(SysUser user) {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
