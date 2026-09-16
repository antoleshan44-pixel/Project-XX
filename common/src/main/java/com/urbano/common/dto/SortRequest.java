package com.urbano.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortRequest {
    private String field;
    private Direction direction = Direction.ASC;

    public enum Direction {
        ASC, DESC
    }
}