package com.leyoswimming.dto.response;

import java.util.List;

public record CoachListResponse(
    List<CoachListItemResponse> items, Long total, Integer page, Integer pageSize) {}
