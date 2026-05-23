package com.webwar.webwar.modules.user.service;

import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.user.model.dto.LoginDTO;
import com.webwar.webwar.modules.user.model.dto.RegisterDTO;
import com.webwar.webwar.modules.user.model.entity.User;

public interface UserService {
    R<?> register(RegisterDTO registerDTO);
    R<?> login(LoginDTO loginDTO);
    User getByUsername(String username);
}