package com.webwar.webwar.modules.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.comment.mapper.CommentMapper;
import com.webwar.webwar.modules.comment.model.dto.CreateCommentDTO;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import com.webwar.webwar.modules.comment.service.CommentService;
import com.webwar.webwar.modules.post.mapper.PostMapper;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.post.service.impl.PostServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final PostServiceImpl postService;

    @Override
    public R<?> createComment(Long postId, CreateCommentDTO dto) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return R.fail("帖子不存在");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        commentMapper.insert(comment);

        postService.incrementCommentCount(postId);
        return R.ok("评论成功");
    }

    @Override
    public R<List<Comment>> getCommentsByPostId(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId).orderByAsc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(wrapper);
        return R.ok(comments);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            commentMapper.deleteById(commentId);
            postService.decrementCommentCount(comment.getPostId());
        }
    }
}