package com.webwar.webwar.modules.board.strategy;

import com.webwar.webwar.modules.board.model.entity.Board;

public interface BoardCreationStrategy {
    Board createBoard(String name, String description, Long creatorId);
}