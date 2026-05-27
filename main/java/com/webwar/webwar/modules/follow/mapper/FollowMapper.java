package com.webwar.webwar.modules.follow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webwar.webwar.modules.follow.model.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}