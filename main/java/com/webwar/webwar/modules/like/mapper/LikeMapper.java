package com.webwar.webwar.modules.like.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webwar.webwar.modules.like.model.entity.Like;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}