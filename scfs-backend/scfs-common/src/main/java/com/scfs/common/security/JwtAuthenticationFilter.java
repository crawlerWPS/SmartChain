package com.scfs.common.security;

import com.scfs.common.constant.ScfsConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器 - 对应 RFC S2-1 AuthService JWT 校验
 *
 * <p>从 Authorization Header 解析 JWT，校验签名/过期，注入 SecurityContext</p>
 *
 * <p>JWT 载荷：{userId, username, roleCode, exp}，有效期 30 分钟</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${scfs.jwt.secret}")
    private String secret;

    @Value("${scfs.jwt.header}")
    private String header;

    @Value("${scfs.jwt.prefix}")
    private String prefix;

    private final SecurityContextHelper securityContextHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(header);
        if (authHeader == null || !authHeader.startsWith(prefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(prefix.length()).trim();
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            String roleCode = claims.get("roleCode", String.class);
            String realName = claims.get("realName", String.class);

            // 注入 ThreadLocal 上下文
            SecurityContextHelper.CurrentUser user =
                    new SecurityContextHelper.CurrentUser(userId, username, realName, roleCode, token);
            user.ipAddress(request.getRemoteAddr());
            securityContextHelper.setCurrentUser(user);

            // 注入 Spring Security Context
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.warn("[JWT] 校验失败: {}", e.getMessage());
            SecurityContextHolder.clear();
            securityContextHelper.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            securityContextHelper.clear();
        }
    }

    /** 生成 JWT token */
    public String generateToken(Long userId, String username, String realName, String roleCode) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long expirationMs = System.currentTimeMillis() + (30 * 60 * 1000L);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("realName", realName)
                .claim("roleCode", roleCode)
                .setSubject(username)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(expirationMs))
                .signWith(key)
                .compact();
    }
}
