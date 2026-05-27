package com.webwar.webwar.modules.post.model.dto;

import lombok.Data;

@Data
public class CreatePostDTO {
    private Long boardId;
    private Long userId;
    private String title;
    private String content;
}