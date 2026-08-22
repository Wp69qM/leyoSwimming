package com.leyoswimming.dto.ai.gateway;

import java.util.List;

public record AiSessionListResponse(
    List<AiSessionListItemResponse> items,
    Long total,
    Integer page,
    Integer size) {
}
