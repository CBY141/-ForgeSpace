package com.webwar.webwar.modules.board.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("boards")
public class Board {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long creatorId;

    private LocalDateTime createdAt;
}