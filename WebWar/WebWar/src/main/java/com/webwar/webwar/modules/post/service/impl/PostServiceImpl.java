package com.webwar.webwar.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.post.mapper.PostMapper;
import com.webwar.webwar.modules.post.model.dto.CreatePostDTO;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    @Override
    public R<?> createPost(CreatePostDTO dto) {
        Post post = new Post();
        post.setBoardId(dto.getBoardId());
        post.setUserId(dto.getUserId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCommentCount(0);
        postMapper.insert(post);
        return R.ok("发帖成功");
    }

    @Override
    public R<List<Post>> getPostsByBoardId(Long boardId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getBoardId, boardId).orderByDesc(Post::getCreatedAt);
        List<Post> posts = postMapper.selectList(wrapper);
        return R.ok(posts);
    }

    @Override
    public R<Post> getPostById(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return R.fail("帖子不存在");
        }
        return R.ok(post);
    }

    // 新增：评论数 +1
    public void incrementCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }
    }
}