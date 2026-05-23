package com.webwar.webwar.modules.post.controller;

import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.post.model.dto.CreatePostDTO;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 发帖
    @PostMapping("/posts")
    public R<?> createPost(@RequestBody CreatePostDTO dto) {
        return postService.createPost(dto);
    }

    // 查看指定板块下的所有帖子
    @GetMapping("/boards/{boardId}/posts")
    public R<List<Post>> getPostsByBoard(@PathVariable Long boardId) {
        return postService.getPostsByBoardId(boardId);
    }

    // 查看帖子详情
    @GetMapping("/posts/{id}")
    public R<Post> getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }
}