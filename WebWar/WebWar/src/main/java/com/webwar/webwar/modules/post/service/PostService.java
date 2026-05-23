package com.webwar.webwar.modules.post.service;

import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.post.model.dto.CreatePostDTO;
import com.webwar.webwar.modules.post.model.entity.Post;

import java.util.List;

public interface PostService {
    R<?> createPost(CreatePostDTO dto);
    R<List<Post>> getPostsByBoardId(Long boardId);
    R<Post> getPostById(Long id);
}