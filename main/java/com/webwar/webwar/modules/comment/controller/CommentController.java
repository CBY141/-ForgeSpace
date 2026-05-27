package com.webwar.webwar.modules.comment.controller;

import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.comment.model.dto.CreateCommentDTO;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import com.webwar.webwar.modules.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 发表评论
    @PostMapping("/posts/{postId}/comments")
    public R<?> createComment(@PathVariable Long postId, @RequestBody CreateCommentDTO dto) {
        return commentService.createComment(postId, dto);
    }

    // 查看帖子的所有评论
    @GetMapping("/posts/{postId}/comments")
    public R<List<Comment>> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPostId(postId);
    }
}