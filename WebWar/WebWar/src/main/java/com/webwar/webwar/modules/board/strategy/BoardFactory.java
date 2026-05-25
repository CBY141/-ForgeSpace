package com.webwar.webwar.modules.board.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BoardFactory {

    private final Map<String, BoardCreationStrategy> strategyMap;

    public BoardCreationStrategy getStrategy(String type) {
        BoardCreationStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            // 默认返回普通策略
            return strategyMap.get("normal");
        }
        return strategy;
    }
}