package com.scfs.common.controller;

import com.scfs.common.core.Result;
import com.scfs.common.security.JwtAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 Controller - 对应 RFC 3.1.x /auth/login /auth/refresh /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtAuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Result.fail(400, "用户名或密码不能为空");
        }
        Map<String, Object> result = authService.login(username, password);
        return Result.ok(result);
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return Result.fail(400, "refreshToken 不能为空");
        }
        return Result.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        authService.logout(refreshToken);
        return Result.ok();
    }
}
