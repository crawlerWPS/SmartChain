package com.scfs.common.service;

import com.scfs.common.core.PageResult;
import com.scfs.common.entity.SysUser;
import com.scfs.common.mapper.SysUserMapper;
import com.scfs.common.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;

/**
 * 用户服务 - 对应 RFC 4.1.1 UserService
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public SysUser getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public PageResult<SysUser> search(String keyword, String roleCode, long offset, int size) {
        long total = userMapper.countAll(keyword, roleCode);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(userMapper.selectPage(keyword, roleCode, offset, size), total);
    }

    @Transactional
    public Long createUser(SysUser user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus((short) 1);
        userMapper.insert(user);
        return user.getId();
    }

    @Transactional
    public void updateUser(SysUser user) {
        SysUser existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (StringUtils.hasText(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        } else {
            user.setPasswordHash(existing.getPasswordHash());
        }
        userMapper.update(user);
    }

    @Transactional
    public void toggleStatus(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        userMapper.updateStatus(id, (short) (user.getStatus() == 1 ? 0 : 1));
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (id.equals(currentUserId)) {
            throw new IllegalStateException("不能删除当前登录用户");
        }
        userMapper.deleteById(id);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
