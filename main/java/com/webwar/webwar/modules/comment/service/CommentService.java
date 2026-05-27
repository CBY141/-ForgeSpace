package com.webwar.webwar.modules.comment.service;

import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.comment.model.dto.CreateCommentDTO;
import com.webwar.webwar.modules.comment.model.entity.Comment;

import java.util.List;

public interface CommentService {
    R<?> createComment(Long postId, CreateCommentDTO dto);
    R<List<Comment>> getCommentsByPostId(Long postId);
}