package com.webwar.webwar.modules.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("posts")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long boardId;

    private Long userId;

    private String title;

    private String content;

    private Integer commentCount;

    private Integer likeCount;

    private Integer status;

    private LocalDateTime createdAt;
}