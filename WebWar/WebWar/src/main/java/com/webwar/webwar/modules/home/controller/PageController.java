package com.webwar.webwar.modules.home.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webwar.webwar.common.exception.BusinessException;
import com.webwar.webwar.modules.board.mapper.BoardMapper;
import com.webwar.webwar.modules.board.model.entity.Board;
import com.webwar.webwar.modules.board.strategy.BoardFactory;
import com.webwar.webwar.modules.comment.model.dto.CreateCommentDTO;
import com.webwar.webwar.modules.comment.model.entity.Comment;
import com.webwar.webwar.modules.comment.mapper.CommentMapper;
import com.webwar.webwar.modules.comment.service.CommentService;
import com.webwar.webwar.modules.comment.service.impl.CommentServiceImpl;
import com.webwar.webwar.modules.post.mapper.PostMapper;
import com.webwar.webwar.modules.post.model.entity.Post;
import com.webwar.webwar.modules.post.service.impl.PostServiceImpl;
import com.webwar.webwar.modules.user.mapper.UserMapper;
import com.webwar.webwar.modules.user.model.dto.LoginDTO;
import com.webwar.webwar.modules.user.model.dto.RegisterDTO;
import com.webwar.webwar.modules.user.model.entity.User;
import com.webwar.webwar.modules.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BoardMapper boardMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserService userService;
    private final CommentService commentService;
    private final BoardFactory boardFactory;
    private final PostServiceImpl postService;
    private final CommentServiceImpl commentServiceImpl;
    private final UserMapper userMapper;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 首页
    @GetMapping("/")
    public String index(Model model) {
        List<Board> boards = boardMapper.selectList(null);
        model.addAttribute("boards", boards);
        return "index";
    }

    // 板块详情
    @GetMapping("/boards/{boardId}")
    public String boardDetail(@PathVariable Long boardId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Board board = boardMapper.selectById(boardId);
        if (board == null) return "redirect:/";
        model.addAttribute("board", board);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getBoardId, boardId).orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        model.addAttribute("posts", postPage.getRecords());
        model.addAttribute("currentPage", postPage.getCurrent());
        model.addAttribute("totalPages", postPage.getPages());
        model.addAttribute("totalPosts", postPage.getTotal());

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

    // 搜索帖子
    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Post::getTitle, keyword)
                .or()
                .like(Post::getContent, keyword)
                .orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);

        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", postPage.getRecords());
        model.addAttribute("currentPage", postPage.getCurrent());
        model.addAttribute("totalPages", postPage.getPages());
        model.addAttribute("totalPosts", postPage.getTotal());
        return "search-result";
    }

    // 用户个人主页
    @GetMapping("/user/{userId}")
    public String userProfile(@PathVariable Long userId, Model model) {
        User profileUser = userService.getById(userId);
        if (profileUser == null) return "redirect:/";
        model.addAttribute("profileUser", profileUser);

        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(Post::getUserId, userId).orderByDesc(Post::getCreatedAt);
        model.addAttribute("posts", postMapper.selectList(postWrapper));

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getUserId, userId).orderByDesc(Comment::getCreatedAt);
        model.addAttribute("comments", commentMapper.selectList(commentWrapper));

        return "profile";
    }

    // 修改头像页面
    @GetMapping("/user/{userId}/avatar")
    public String editAvatarPage(@PathVariable Long userId, Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;

        User user = userService.getById(userId);
        model.addAttribute("profileUser", user);
        return "edit-avatar";
    }

    // 处理头像上传
    @PostMapping("/user/{userId}/avatar")
    public String editAvatar(@PathVariable Long userId,
                             @RequestParam("file") MultipartFile file,
                             HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;

        if (!file.isEmpty()) {
            try {
                File avatarDir = new File(uploadDir + "avatars/");
                if (!avatarDir.exists()) {
                    avatarDir.mkdirs();
                }

                String originalFilename = file.getOriginalFilename();
                String suffix = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFileName = UUID.randomUUID().toString() + suffix;

                File dest = new File(avatarDir, newFileName);
                file.transferTo(dest);

                User user = userMapper.selectById(userId);
                user.setAvatar("/avatars/" + newFileName);
                userMapper.updateById(user);
                session.setAttribute("user", user);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/user/" + userId;
    }

    // 恢复默认头像
    @GetMapping("/user/{userId}/avatar/default")
    public String resetAvatar(@PathVariable Long userId, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;

        User user = userMapper.selectById(userId);
        if (user != null) {
            String seed = user.getUsername() + "_" + System.currentTimeMillis();
            user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=" + seed);
            userMapper.updateById(user);
            session.setAttribute("user", user);
        }
        return "redirect:/user/" + userId;
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
        try {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername(username);
            dto.setPassword(password);
            dto.setNickname(nickname);
            userService.register(dto);
            return "redirect:/login";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
        try {
            LoginDTO dto = new LoginDTO();
            dto.setUsername(username);
            dto.setPassword(password);
            userService.login(dto);
            User user = userService.getByUsername(username);
            session.setAttribute("user", user);
            return "redirect:/";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
        post.setCommentCount(0);
        postMapper.insert(post);
        return "redirect:/boards/" + boardId;
    }

    // 创建板块
    @PostMapping("/boards")
    public String createBoard(@RequestParam String name,
                              @RequestParam String description,
                              @RequestParam(defaultValue = "normal") String type,
                              HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Board board = boardFactory.getStrategy(type)
                .createBoard(name, description, user.getId());
        boardMapper.insert(board);
        return "redirect:/";
    }

    // 管理员删除板块
    @GetMapping("/boards/{boardId}/delete")
    public String deleteBoard(@PathVariable Long boardId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!user.isAdmin()) return "redirect:/";

        boardMapper.deleteById(boardId);
        return "redirect:/";
    }

    // 发表评论
    @PostMapping("/posts/{postId}/comments")
    public String createComment(@PathVariable Long postId,
                                @RequestParam String content,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        CreateCommentDTO dto = new CreateCommentDTO();
        dto.setUserId(user.getId());
        dto.setContent(content);
        commentService.createComment(postId, dto);
        return "redirect:/posts/" + postId;
    }

    // 编辑帖子页面
    @GetMapping("/posts/{postId}/edit")
    public String editPostPage(@PathVariable Long postId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Post post = postMapper.selectById(postId);
        if (post == null) return "redirect:/";

        if (!post.getUserId().equals(user.getId())) {
            return "redirect:/posts/" + postId;
        }

        model.addAttribute("post", post);
        return "post-edit";
    }

    // 处理编辑提交
    @PostMapping("/posts/{postId}/edit")
    public String editPost(@PathVariable Long postId,
                           @RequestParam String title,
                           @RequestParam String content,
                           HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Post post = postMapper.selectById(postId);
        if (post == null) return "redirect:/";

        if (!post.getUserId().equals(user.getId())) {
            return "redirect:/posts/" + postId;
        }

        post.setTitle(title);
        post.setContent(content);
        postMapper.updateById(post);

        return "redirect:/posts/" + postId;
    }

    // 删除帖子（作者或管理员可删）
    @GetMapping("/posts/{postId}/delete")
    public String deletePost(@PathVariable Long postId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Post post = postMapper.selectById(postId);
        if (post != null && (post.getUserId().equals(user.getId()) || user.isAdmin())) {
            postService.deletePost(postId);
        }
        return "redirect:/boards/" + post.getBoardId();
    }

    // 删除评论（作者或管理员可删）
    @GetMapping("/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Comment comment = commentMapper.selectById(commentId);
        if (comment != null && (comment.getUserId().equals(user.getId()) || user.isAdmin())) {
            commentServiceImpl.deleteComment(commentId);
            return "redirect:/posts/" + comment.getPostId();
        }
        return "redirect:/";
    }
}