package com.webwar.webwar.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
            return R.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        userMapper.insert(user);
        return R.ok("注册成功");
    }

    @Override
    public R<?> login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        if (user == null) {
            return R.fail("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return R.fail("用户名或密码错误");
        }
        return R.ok("登录成功");
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }
}