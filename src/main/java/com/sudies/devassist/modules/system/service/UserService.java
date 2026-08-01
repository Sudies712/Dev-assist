package com.sudies.devassist.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sudies.devassist.common.enums.UserStatus;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.modules.system.dto.CreateUserDTO;
import com.sudies.devassist.modules.system.dto.UpdateUserDTO;
import com.sudies.devassist.modules.system.dto.UserQuery;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.entity.UserRole;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.system.mapper.UserRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理（系统管理员）：分页查询、新增/编辑、启用禁用、重置密码、分配角色。
 */
@Service
public class UserService {

    private static final String DEFAULT_PASSWORD = "123456";

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageResult<User> page(UserQuery q) {
        Page<User> page = new Page<>(q.getPage(), q.getPageSize());
        var w = Wrappers.<User>lambdaQuery();
        if (StringUtils.hasText(q.getUsername())) {
            w.like(User::getUsername, q.getUsername());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(User::getStatus, q.getStatus());
        }
        w.orderByDesc(User::getCreateTime);
        userMapper.selectPage(page, w);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
    }

    public void create(CreateUserDTO dto) {
        Long cnt = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (cnt != null && cnt > 0L) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setRealName(dto.getRealName());
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());
        u.setStatus(UserStatus.ENABLED.name());
        u.setLoginFailCount(0);
        userMapper.insert(u);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(u.getId(), dto.getRoleIds());
        }
    }

    public void update(Long id, UpdateUserDTO dto) {
        User u = new User();
        u.setId(id);
        u.setRealName(dto.getRealName());
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());
        userMapper.updateById(u);
    }

    public void changeStatus(Long id, String status) {
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    public String resetPassword(Long id) {
        User u = new User();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        u.setLoginFailCount(0);
        u.setLockUntil(null);
        userMapper.updateById(u);
        return DEFAULT_PASSWORD;
    }

    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long rid : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
    }
}
