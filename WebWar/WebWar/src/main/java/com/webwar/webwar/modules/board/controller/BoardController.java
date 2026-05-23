package com.webwar.webwar.modules.board.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webwar.webwar.common.result.R;
import com.webwar.webwar.modules.board.mapper.BoardMapper;
import com.webwar.webwar.modules.board.model.entity.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardMapper boardMapper;

    // 创建板块
    @PostMapping
    public R<?> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.getOrDefault("description", "");

        // 检查板块名是否已存在
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Board::getName, name);
        Board existBoard = boardMapper.selectOne(wrapper);
        if (existBoard != null) {
            return R.fail("板块名已存在");
        }

        Board board = new Board();
        board.setName(name);
        board.setDescription(description);
        boardMapper.insert(board);

        return R.ok("板块创建成功");
    }

    // 获取所有板块
    @GetMapping
    public R<List<Board>> list() {
        List<Board> boards = boardMapper.selectList(null);
        return R.ok(boards);
    }
}