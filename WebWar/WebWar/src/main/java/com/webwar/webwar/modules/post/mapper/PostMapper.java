package com.webwar.webwar.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webwar.webwar.modules.post.model.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT * FROM posts WHERE title LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<Post> searchByTitle(String keyword);
}