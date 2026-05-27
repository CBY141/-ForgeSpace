package com.webwar.webwar.modules.board.strategy;

import com.webwar.webwar.modules.board.model.entity.Board;
import org.springframework.stereotype.Component;

@Component("normal")
public class NormalBoardStrategy implements BoardCreationStrategy {

    @Override
    public Board createBoard(String name, String description, Long creatorId) {
        Board board = new Board();
        board.setName(name);
        board.setDescription(description);
        board.setCreatorId(creatorId);
        board.setType("normal");
        board.setIsOfficial(0);
        return board;
    }
}