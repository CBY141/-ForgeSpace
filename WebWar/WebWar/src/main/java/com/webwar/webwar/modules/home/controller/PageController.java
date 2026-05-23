package com.webwar.webwar.modules.home.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.modules.board.mapper.BoardMapper;
import com.webwar.webwar.modules.board.model.entity.Board;
import com.webwar.webwar.modules.comment.mapper.CommentMapper;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import com.webwar.webwar.modules.post.mapper.PostMapper;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.user.model.dto.LoginDTO;
import com.webwar.webwar.modules.user.model.dto.RegisterDTO;
import com.webwar.webwar.modules.user.model.entity.User;
import com.webwar.webwar.modules.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BoardMapper boardMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserService userService;

    // 首页
    @GetMapping("/")
    public String index(Model model) {
        List<Board> boards = boardMapper.selectList(null);
        model.addAttribute("boards", boards);
        return "index";
    }

    // 板块详情
    @GetMapping("/boards/{boardId}")
    public String boardDetail(@PathVariable Long boardId, Model model) {
        Board board = boardMapper.selectById(boardId);
        if (board == null) return "redirect:/";
        model.addAttribute("board", board);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getBoardId, boardId).orderByDesc(Post::getCreatedAt);
        model.addAttribute("posts", postMapper.selectList(wrapper));
        return "board-detail";
    }

    // 帖子详情
    @GetMapping("/posts/{postId}")
    public String postDetail(@PathVariable Long postId, Model model) {
        Post post = postMapper.selectById(postId);
        if (post == null) return "redirect:/";
        model.addAttribute("post", post);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId).orderByAsc(Comment::getCreatedAt);
        model.addAttribute("comments", commentMapper.selectList(wrapper));
        return "post-detail";
    }

    // 注册页面
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    // 处理注册
    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(required = false) String nickname,
                           RedirectAttributes redirectAttributes) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setNickname(nickname);
        var result = userService.register(dto);
        if (result.getCode() == 200) {
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
            return "redirect:/register";
        }
    }

    // 登录页面
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    // 处理登录
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        var result = userService.login(dto);
        if (result.getCode() == 200) {
            User user = userService.getByUsername(username);
            session.setAttribute("user", user);
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
            return "redirect:/login";
        }
    }

    // 退出
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/";
    }

    // 发帖
    @PostMapping("/boards/{boardId}/posts")
    public String createPost(@PathVariable Long boardId,
                             @RequestParam String title,
                             @RequestParam String content,
                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Post post = new Post();
        post.setBoardId(boardId);
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        postMapper.insert(post);
        return "redirect:/boards/" + boardId;
    }

    // 发表评论
    @PostMapping("/posts/{postId}/comments")
    public String createComment(@PathVariable Long postId,
                                @RequestParam String content,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(user.getId());
        comment.setContent(content);
        commentMapper.insert(comment);
        return "redirect:/posts/" + postId;
    }
}