package com.webwar.webwar.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}