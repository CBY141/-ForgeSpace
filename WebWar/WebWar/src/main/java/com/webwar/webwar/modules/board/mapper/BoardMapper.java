package com.webwar.webwar.modules.board.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webwar.webwar.modules.board.model.entity.Board;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BoardMapper extends BaseMapper<Board> {
}