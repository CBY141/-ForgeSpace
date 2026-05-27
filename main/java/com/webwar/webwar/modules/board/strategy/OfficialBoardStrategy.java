package com.webwar.webwar.modules.board.strategy;

import com.webwar.webwar.modules.board.model.entity.Board;
import org.springframework.stereotype.Component;

@Component("official")
public class OfficialBoardStrategy implements BoardCreationStrategy {

    @Override
    public Board createBoard(String name, String description, Long creatorId) {
        Board board = new Board();
        board.setName("【官方】" + name);
        board.setDescription(description);
        board.setCreatorId(creatorId);
        board.setType("official");
        board.setIsOfficial(1);
        return board;
    }
}