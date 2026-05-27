package com.webwar.webwar.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.common.exception.BusinessException;
import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.comment.mapper.CommentMapper;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import com.webwar.webwar.modules.post.mapper.PostMapper;
import com.webwar.webwar.modules.post.model.dto.CreatePostDTO;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    public R<?> createPost(CreatePostDTO dto) {
        Post post = new Post();
        post.setBoardId(dto.getBoardId());
        post.setUserId(dto.getUserId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCommentCount(0);
        post.setLikeCount(0);
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
            throw new BusinessException(404, "帖子不存在");
        }
        return R.ok(post);
    }

    public void incrementCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);
    }

    public void decrementCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
    }

    public void incrementLikeCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
        }
    }

    public void decrementLikeCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId);
        commentMapper.delete(wrapper);
        postMapper.deleteById(postId);
    }
}