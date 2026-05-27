package com.webwar.webwar.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.common.exception.BusinessException;
import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.user.mapper.UserMapper;
import com.webwar.webwar.modules.user.model.dto.LoginDTO;
import com.webwar.webwar.modules.user.model.dto.RegisterDTO;
import com.webwar.webwar.modules.user.model.entity.User;
import com.webwar.webwar.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public R<?> register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());

        // 用“用户名+时间戳”确保字母种子，避免纯数字
        String seed = dto.getUsername() + "_" + System.currentTimeMillis();
        user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=" + seed);
        userMapper.insert(user);
        return R.ok("注册成功");
    }

    @Override
    public R<?> login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        return R.ok("登录成功");
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}