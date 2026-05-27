package com.webwar.webwar.modules.comment.model.dto;

import lombok.Data;

@Data
public class CreateCommentDTO {
    private Long userId;
    private String content;
}