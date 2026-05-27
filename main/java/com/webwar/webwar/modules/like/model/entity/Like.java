package com.webwar.webwar.modules.like.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("likes")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long postId;

    private LocalDateTime createdAt;
}