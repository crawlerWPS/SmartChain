package com.scfs.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.constant.ScfsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 安全上下文工具 - 当前登录用户/角色/API权限缓存
 *
 * <p>JWT 解析后存入 ThreadLocal SecurityContext，配合 Redis 缓存角色权限</p>
 *
 * <p>对应 RFC S2-1：JWT 签发/校验、登录/登出</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    public CurrentUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public void setCurrentUser(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public void clear() {
        CURRENT_USER.remove();
    }

    /** 获取当前用户 ID（如未登录抛异常） */
    public Long getCurrentUserIdOrThrow() {
        CurrentUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("当前未登录");
        }
        return user.userId();
    }

    /** 获取当前用户角色 */
    public String getCurrentRoleCodeOrThrow() {
        CurrentUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("当前未登录");
        }
        return user.roleCode();
    }

    /** 从缓存获取角色对应的 API 权限（如无则下次登录重新加载） */
    public Map<String, List<String>> getRolePermissions(String roleCode) {
        String cacheKey = ScfsConstants.CACHE_USER_PERMISSION + roleCode;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            log.warn("[Permission] 角色权限缓存未命中: {}", roleCode);
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(cached,
                    new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            log.error("[Permission] 解析角色权限失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 缓存角色权限（30分钟） */
    public void cacheRolePermissions(String roleCode, Map<String, List<String>> permissions) {
        try {
            String cacheKey = ScfsConstants.CACHE_USER_PERMISSION + roleCode;
            String json = objectMapper.writeValueAsString(permissions);
            redisTemplate.opsForValue().set(cacheKey, json, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[Permission] 缓存角色权限失败: {}", e.getMessage());
        }
    }

    /** 清除角色权限缓存（角色权限变更时调用） */
    public void evictRolePermissionCache(String roleCode) {
        redisTemplate.delete(ScfsConstants.CACHE_USER_PERMISSION + roleCode);
    }

    /**
     * 当前登录用户上下文 - ThreadLocal 存储
     */
    public static class CurrentUser {
        private final Long userId;
        private final String username;
        private final String realName;
        private final String roleCode;
        private final String token;
        private String ipAddress;

        public CurrentUser(Long userId, String username, String realName, String roleCode, String token) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.roleCode = roleCode;
            this.token = token;
        }

        public Long userId() { return userId; }
        public String username() { return username; }
        public String realName() { return realName; }
        public String roleCode() { return roleCode; }
        public String token() { return token; }
        public String ipAddress() { return ipAddress; }
        public void ipAddress(String ip) { this.ipAddress = ip; }
    }
}
