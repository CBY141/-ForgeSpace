package com.webwar.webwar.common;

import com.webwar.webwar.modules.user.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class UserNicknameUtil {

    private final UserMapper userMapper;
    private final Map<Long, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        userMapper.selectList(null).forEach(user -> cache.put(user.getId(), user.getNickname()));
    }

    public String getNickname(Long userId) {
        if (userId == null) return "匿名";
        return cache.computeIfAbsent(userId, id -> {
            var user = userMapper.selectById(id);
            return user != null ? user.getNickname() : "未知用户";
        });
    }
}