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
import com.webwar.webwar.modules.follow.mapper.FollowMapper;
import com.webwar.webwar.modules.follow.model.entity.Follow;
import com.webwar.webwar.modules.like.mapper.LikeMapper;
import com.webwar.webwar.modules.like.model.entity.Like;
import com.webwar.webwar.modules.message.mapper.MessageMapper;
import com.webwar.webwar.modules.message.model.entity.Message;
import com.webwar.webwar.modules.message.websocket.ChatWebSocketHandler;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
    private final FollowMapper followMapper;
    private final LikeMapper likeMapper;
    private final MessageMapper messageMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 将当前登录用户放入 Model，供所有模板使用
    private void addCurrentUser(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("currentUser", user);
    }

    // 未读消息数
    private void addUnreadCount(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<>();
            unreadWrapper.eq(Message::getReceiverId, user.getId()).eq(Message::getIsRead, 0);
            model.addAttribute("unreadCount", messageMapper.selectCount(unreadWrapper));
        }
    }

    // ==================== 首页 ====================
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Board::getStatus, 1);
        model.addAttribute("boards", boardMapper.selectList(wrapper));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "index";
    }

    // ==================== 板块详情 ====================
    @GetMapping("/boards/{boardId}")
    public String boardDetail(@PathVariable Long boardId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model, HttpSession session) {
        Board board = boardMapper.selectById(boardId);
        if (board == null) return "redirect:/";
        model.addAttribute("board", board);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getBoardId, boardId).eq(Post::getStatus, 1).orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);
        model.addAttribute("posts", postPage.getRecords());
        model.addAttribute("currentPage", postPage.getCurrent());
        model.addAttribute("totalPages", postPage.getPages());
        model.addAttribute("totalPosts", postPage.getTotal());
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "board-detail";
    }

    // ==================== 帖子详情 ====================
    @GetMapping("/posts/{postId}")
    public String postDetail(@PathVariable Long postId, Model model, HttpSession session) {
        Post post = postMapper.selectById(postId);
        if (post == null) return "redirect:/";
        model.addAttribute("post", post);

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getPostId, postId).orderByAsc(Comment::getCreatedAt);
        model.addAttribute("comments", commentMapper.selectList(commentWrapper));

        User sessionUser = (User) session.getAttribute("user");
        boolean isLiked = false;
        if (sessionUser != null) {
            LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(Like::getUserId, sessionUser.getId()).eq(Like::getPostId, postId);
            isLiked = likeMapper.selectCount(likeWrapper) > 0;
        }
        model.addAttribute("isLiked", isLiked);
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "post-detail";
    }

    // ==================== 点赞/取消点赞 ====================
    @GetMapping("/posts/{postId}/like")
    public String likePost(@PathVariable Long postId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, user.getId()).eq(Like::getPostId, postId);
        if (likeMapper.selectCount(wrapper) == 0) {
            Like like = new Like();
            like.setUserId(user.getId());
            like.setPostId(postId);
            likeMapper.insert(like);
            postService.incrementLikeCount(postId);
        }
        return "redirect:/posts/" + postId;
    }

    @GetMapping("/posts/{postId}/unlike")
    public String unlikePost(@PathVariable Long postId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, user.getId()).eq(Like::getPostId, postId);
        if (likeMapper.selectCount(wrapper) > 0) {
            likeMapper.delete(wrapper);
            postService.decrementLikeCount(postId);
        }
        return "redirect:/posts/" + postId;
    }

    // ==================== 搜索 ====================
    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model, HttpSession session) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Post::getTitle, keyword).or().like(Post::getContent, keyword)
                .eq(Post::getStatus, 1).orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size), wrapper);
        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", postPage.getRecords());
        model.addAttribute("currentPage", postPage.getCurrent());
        model.addAttribute("totalPages", postPage.getPages());
        model.addAttribute("totalPosts", postPage.getTotal());
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "search-result";
    }

    // ==================== 用户主页 ====================
    @GetMapping("/user/{userId}")
    public String userProfile(@PathVariable Long userId, Model model, HttpSession session) {
        User profileUser = userService.getById(userId);
        if (profileUser == null) return "redirect:/";
        model.addAttribute("profileUser", profileUser);

        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(Post::getUserId, userId).eq(Post::getStatus, 1).orderByDesc(Post::getCreatedAt);
        model.addAttribute("posts", postMapper.selectList(postWrapper));

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getUserId, userId).orderByDesc(Comment::getCreatedAt);
        model.addAttribute("comments", commentMapper.selectList(commentWrapper));

        LambdaQueryWrapper<Follow> followeeWrapper = new LambdaQueryWrapper<>();
        followeeWrapper.eq(Follow::getFollowerId, userId);
        model.addAttribute("followingCount", followMapper.selectCount(followeeWrapper));

        LambdaQueryWrapper<Follow> followerWrapper = new LambdaQueryWrapper<>();
        followerWrapper.eq(Follow::getFolloweeId, userId);
        model.addAttribute("followerCount", followMapper.selectCount(followerWrapper));

        User sessionUser = (User) session.getAttribute("user");
        boolean isFollowing = false;
        if (sessionUser != null && !sessionUser.getId().equals(userId)) {
            LambdaQueryWrapper<Follow> isFollowWrapper = new LambdaQueryWrapper<>();
            isFollowWrapper.eq(Follow::getFollowerId, sessionUser.getId()).eq(Follow::getFolloweeId, userId);
            isFollowing = followMapper.selectCount(isFollowWrapper) > 0;
        }
        model.addAttribute("isFollowing", isFollowing);
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "profile";
    }

    // ==================== 我的帖子/我的板块 ====================
    @GetMapping("/my-posts")
    public String myPosts(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, user.getId()).orderByDesc(Post::getCreatedAt);
        model.addAttribute("posts", postMapper.selectList(wrapper));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "my-posts";
    }

    @GetMapping("/my-boards")
    public String myBoards(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Board::getCreatorId, user.getId()).orderByDesc(Board::getCreatedAt);
        model.addAttribute("boards", boardMapper.selectList(wrapper));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "my-boards";
    }

    // ==================== 个人设置 ====================
    @GetMapping("/user/{userId}/settings")
    public String settingsPage(@PathVariable Long userId, Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;
        model.addAttribute("profileUser", userService.getById(userId));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "settings";
    }

    @PostMapping("/user/{userId}/settings")
    public String handleSettings(@PathVariable Long userId, @RequestParam String action,
                                 @RequestParam(required = false) String nickname,
                                 @RequestParam(required = false) String oldPassword,
                                 @RequestParam(required = false) String newPassword,
                                 HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;
        User user = userMapper.selectById(userId);
        model.addAttribute("profileUser", user);
        if ("nickname".equals(action) && nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
            userMapper.updateById(user);
            session.setAttribute("user", user);
            model.addAttribute("success", "昵称修改成功！");
        } else if ("password".equals(action)) {
            if (oldPassword == null || newPassword == null || newPassword.isBlank()) {
                model.addAttribute("pwdError", "新密码不能为空");
            } else if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                model.addAttribute("pwdError", "旧密码错误");
            } else {
                user.setPassword(passwordEncoder.encode(newPassword));
                userMapper.updateById(user);
                session.setAttribute("user", user);
                model.addAttribute("pwdSuccess", "密码修改成功！");
            }
        }
        return "settings";
    }

    // ==================== 关注/取消关注 ====================
    @GetMapping("/user/{userId}/follow")
    public String followUser(@PathVariable Long userId, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, sessionUser.getId()).eq(Follow::getFolloweeId, userId);
        if (followMapper.selectCount(wrapper) == 0) {
            Follow follow = new Follow();
            follow.setFollowerId(sessionUser.getId());
            follow.setFolloweeId(userId);
            followMapper.insert(follow);
        }
        return "redirect:/user/" + userId;
    }

    @GetMapping("/user/{userId}/unfollow")
    public String unfollowUser(@PathVariable Long userId, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, sessionUser.getId()).eq(Follow::getFolloweeId, userId);
        followMapper.delete(wrapper);
        return "redirect:/user/" + userId;
    }

    @GetMapping("/user/{userId}/following")
    public String followingList(@PathVariable Long userId, Model model, HttpSession session) {
        User profileUser = userService.getById(userId);
        if (profileUser == null) return "redirect:/";
        model.addAttribute("profileUser", profileUser);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId).orderByDesc(Follow::getCreatedAt);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<User> followingUsers = follows.stream().map(f -> userMapper.selectById(f.getFolloweeId())).filter(u -> u != null).toList();
        model.addAttribute("users", followingUsers);
        model.addAttribute("title", "关注列表");
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "follow-list";
    }

    @GetMapping("/user/{userId}/followers")
    public String followerList(@PathVariable Long userId, Model model, HttpSession session) {
        User profileUser = userService.getById(userId);
        if (profileUser == null) return "redirect:/";
        model.addAttribute("profileUser", profileUser);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFolloweeId, userId).orderByDesc(Follow::getCreatedAt);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<User> followerUsers = follows.stream().map(f -> userMapper.selectById(f.getFollowerId())).filter(u -> u != null).toList();
        model.addAttribute("users", followerUsers);
        model.addAttribute("title", "粉丝列表");
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "follow-list";
    }

    // ==================== 头像 ====================
    @GetMapping("/user/{userId}/avatar")
    public String editAvatarPage(@PathVariable Long userId, Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;
        model.addAttribute("profileUser", userService.getById(userId));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "edit-avatar";
    }

    @PostMapping("/user/{userId}/avatar")
    public String editAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        if (!sessionUser.getId().equals(userId)) return "redirect:/user/" + userId;
        if (!file.isEmpty()) {
            try {
                File avatarDir = new File(uploadDir + "avatars/");
                if (!avatarDir.exists()) avatarDir.mkdirs();
                String originalFilename = file.getOriginalFilename();
                String suffix = "";
                if (originalFilename != null && originalFilename.contains(".")) suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFileName = UUID.randomUUID().toString() + suffix;
                File dest = new File(avatarDir, newFileName);
                file.transferTo(dest);
                User user = userMapper.selectById(userId);
                user.setAvatar("/avatars/" + newFileName);
                userMapper.updateById(user);
                session.setAttribute("user", user);
            } catch (IOException e) { e.printStackTrace(); }
        }
        return "redirect:/user/" + userId;
    }

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

    // ==================== 注册/登录/退出 ====================
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam(required = false) String nickname, RedirectAttributes ra) {
        try {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername(username); dto.setPassword(password); dto.setNickname(nickname);
            userService.register(dto);
            return "redirect:/login";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes ra) {
        try {
            LoginDTO dto = new LoginDTO();
            dto.setUsername(username); dto.setPassword(password);
            userService.login(dto);
            User user = userService.getByUsername(username);
            session.setAttribute("user", user);
            return "redirect:/";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/";
    }

    // ==================== 发帖/板块/评论/编辑/删除 ====================
    @PostMapping("/boards/{boardId}/posts")
    public String createPost(@PathVariable Long boardId, @RequestParam String title, @RequestParam String content,
                             HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Board board = boardMapper.selectById(boardId);
        if (board == null) return "redirect:/";
        if ("official".equals(board.getType()) && !user.isAdmin()) return "redirect:/boards/" + boardId;
        Post post = new Post();
        post.setBoardId(boardId); post.setUserId(user.getId()); post.setTitle(title); post.setContent(content);
        post.setCommentCount(0); post.setLikeCount(0);
        if (user.isAdmin()) { post.setStatus(1); ra.addFlashAttribute("postSuccess", "发布成功！"); }
        else { post.setStatus(0); ra.addFlashAttribute("postSuccess", "发布成功，请等待管理员审核！"); }
        postMapper.insert(post);
        return "redirect:/boards/" + boardId;
    }

    @PostMapping("/boards")
    public String createBoard(@RequestParam String name, @RequestParam String description,
                              @RequestParam(defaultValue = "normal") String type, HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if ("official".equals(type) && !user.isAdmin()) { ra.addFlashAttribute("boardError", "官方板块仅管理员可创建！"); return "redirect:/"; }
        Board board = boardFactory.getStrategy(type).createBoard(name, description, user.getId());
        if (user.isAdmin()) { board.setStatus(1); ra.addFlashAttribute("boardSuccess", "板块创建成功！"); }
        else { board.setStatus(0); ra.addFlashAttribute("boardSuccess", "板块申请已提交，请等待管理员审核！"); }
        boardMapper.insert(board);
        return "redirect:/";
    }

    @PostMapping("/posts/{postId}/comments")
    public String createComment(@PathVariable Long postId, @RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        CreateCommentDTO dto = new CreateCommentDTO();
        dto.setUserId(user.getId()); dto.setContent(content);
        commentService.createComment(postId, dto);
        return "redirect:/posts/" + postId;
    }

    @GetMapping("/posts/{postId}/edit")
    public String editPostPage(@PathVariable Long postId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(user.getId())) return "redirect:/posts/" + postId;
        model.addAttribute("post", post);
        return "post-edit";
    }

    @PostMapping("/posts/{postId}/edit")
    public String editPost(@PathVariable Long postId, @RequestParam String title, @RequestParam String content, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(user.getId())) return "redirect:/posts/" + postId;
        post.setTitle(title); post.setContent(content);
        postMapper.updateById(post);
        return "redirect:/posts/" + postId;
    }

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

    // ==================== 管理员审核 ====================
    @GetMapping("/admin/review")
    public String reviewPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0).orderByDesc(Post::getCreatedAt);
        model.addAttribute("posts", postMapper.selectList(wrapper));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "admin-review";
    }

    @GetMapping("/admin/review/{postId}/approve")
    public String approvePost(@PathVariable Long postId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        Post post = postMapper.selectById(postId);
        if (post != null) { post.setStatus(1); postMapper.updateById(post); }
        return "redirect:/admin/review";
    }

    @GetMapping("/admin/review/{postId}/reject")
    public String rejectPost(@PathVariable Long postId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        Post post = postMapper.selectById(postId);
        if (post != null) { post.setStatus(2); postMapper.updateById(post); }
        return "redirect:/admin/review";
    }

    @GetMapping("/admin/board-review")
    public String boardReviewPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Board::getStatus, 0).orderByDesc(Board::getCreatedAt);
        model.addAttribute("boards", boardMapper.selectList(wrapper));
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "admin-board-review";
    }

    @GetMapping("/admin/board-review/{boardId}/approve")
    public String approveBoard(@PathVariable Long boardId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        Board board = boardMapper.selectById(boardId);
        if (board != null) { board.setStatus(1); boardMapper.updateById(board); }
        return "redirect:/admin/board-review";
    }

    @GetMapping("/admin/board-review/{boardId}/reject")
    public String rejectBoard(@PathVariable Long boardId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        Board board = boardMapper.selectById(boardId);
        if (board != null) { board.setStatus(2); boardMapper.updateById(board); }
        return "redirect:/admin/board-review";
    }

    @GetMapping("/boards/{boardId}/delete")
    public String deleteBoard(@PathVariable Long boardId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) return "redirect:/";
        boardMapper.deleteById(boardId);
        return "redirect:/";
    }

    // ==================== 私聊列表 ====================
    @GetMapping("/messages")
    public String messagesPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        LambdaQueryWrapper<Message> sentWrapper = new LambdaQueryWrapper<>();
        sentWrapper.eq(Message::getSenderId, user.getId());
        List<Message> sentMessages = messageMapper.selectList(sentWrapper);

        LambdaQueryWrapper<Message> receivedWrapper = new LambdaQueryWrapper<>();
        receivedWrapper.eq(Message::getReceiverId, user.getId());
        List<Message> receivedMessages = messageMapper.selectList(receivedWrapper);

        Map<Long, Message> chatMap = new LinkedHashMap<>();
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(sentMessages);
        allMessages.addAll(receivedMessages);
        allMessages.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        for (Message msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(user.getId()) ? msg.getReceiverId() : msg.getSenderId();
            chatMap.putIfAbsent(otherUserId, msg);
        }

        List<Map<String, Object>> chats = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : chatMap.entrySet()) {
            User otherUser = userMapper.selectById(entry.getKey());
            if (otherUser != null) {
                Map<String, Object> chat = new HashMap<>();
                chat.put("user", otherUser);
                chat.put("lastMessage", entry.getValue());
                LambdaQueryWrapper<Message> unreadChatWrapper = new LambdaQueryWrapper<>();
                unreadChatWrapper.eq(Message::getSenderId, otherUser.getId())
                        .eq(Message::getReceiverId, user.getId())
                        .eq(Message::getIsRead, 0);
                chat.put("unreadCount", messageMapper.selectCount(unreadChatWrapper));
                chats.add(chat);
            }
        }

        model.addAttribute("chats", chats);
        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "messages";
    }

    // ==================== 聊天详情页 ====================
    @GetMapping("/chat/{otherUserId}")
    public String chatPage(@PathVariable Long otherUserId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        User otherUser = userMapper.selectById(otherUserId);
        if (otherUser == null) return "redirect:/messages";

        // 标记对方发给我的消息为已读
        LambdaQueryWrapper<Message> markReadWrapper = new LambdaQueryWrapper<>();
        markReadWrapper.eq(Message::getSenderId, otherUserId)
                .eq(Message::getReceiverId, user.getId())
                .eq(Message::getIsRead, 0);
        List<Message> unreadMessages = messageMapper.selectList(markReadWrapper);
        for (Message msg : unreadMessages) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }

        // 获取双方所有消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Message::getSenderId, user.getId()).eq(Message::getReceiverId, otherUserId))
                .or(w2 -> w2.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, user.getId()))
        ).orderByAsc(Message::getCreatedAt);
        List<Message> messages = messageMapper.selectList(wrapper);

        boolean mutualFollow = isMutualFollow(user.getId(), otherUserId);
        long mySentCount = messages.stream().filter(m -> m.getSenderId().equals(user.getId())).count();
        boolean canSend = mutualFollow || mySentCount < 3;

        model.addAttribute("otherUser", otherUser);
        model.addAttribute("messages", messages);
        model.addAttribute("mutualFollow", mutualFollow);
        model.addAttribute("canSend", canSend);
        model.addAttribute("remaining", mutualFollow ? -1 : (int)(3 - mySentCount));

        // 直接传递用户 ID
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("otherUserId", otherUserId);

        addCurrentUser(model, session);
        addUnreadCount(model, session);
        return "chat";
    }

    // ==================== 获取新消息（轮询） ====================
    @GetMapping("/chat/{otherUserId}/new-messages")
    @ResponseBody
    public List<Message> getNewMessages(@PathVariable Long otherUserId,
                                        @RequestParam(defaultValue = "0") Long sinceId,
                                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Collections.emptyList();

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .and(w1 -> w1.eq(Message::getSenderId, user.getId()).eq(Message::getReceiverId, otherUserId))
                        .or(w2 -> w2.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, user.getId()))
                )
                .gt(Message::getId, sinceId)
                .orderByAsc(Message::getId);
        return messageMapper.selectList(wrapper);
    }

    // ==================== 发送消息 ====================
    @PostMapping("/chat/{otherUserId}/send")
    public String sendMessage(@PathVariable Long otherUserId,
                              @RequestParam String content,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        boolean mutualFollow = isMutualFollow(user.getId(), otherUserId);

        if (!mutualFollow && content.length() > 50) {
            redirectAttributes.addFlashAttribute("msgError", "陌生人消息不能超过50字！");
            return "redirect:/chat/" + otherUserId;
        }

        if (!mutualFollow) {
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getSenderId, user.getId()).eq(Message::getReceiverId, otherUserId);
            long sentCount = messageMapper.selectCount(wrapper);
            if (sentCount >= 3) {
                redirectAttributes.addFlashAttribute("msgError", "对方尚未回复，你最多只能发3条消息！");
                return "redirect:/chat/" + otherUserId;
            }
        }

        Message message = new Message();
        message.setSenderId(user.getId());
        message.setReceiverId(otherUserId);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "newMessage");
            payload.put("senderId", user.getId());
            payload.put("senderName", user.getNickname());
            payload.put("content", content);
            payload.put("time", message.getCreatedAt().toString());
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            chatWebSocketHandler.sendMessage(otherUserId, json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/chat/" + otherUserId;
    }

    private boolean isMutualFollow(Long userId1, Long userId2) {
        LambdaQueryWrapper<Follow> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Follow::getFollowerId, userId1).eq(Follow::getFolloweeId, userId2);
        LambdaQueryWrapper<Follow> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Follow::getFollowerId, userId2).eq(Follow::getFolloweeId, userId1);
        return followMapper.selectCount(wrapper1) > 0 && followMapper.selectCount(wrapper2) > 0;
    }
}