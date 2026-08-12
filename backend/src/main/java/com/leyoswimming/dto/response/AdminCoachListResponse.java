package com.leyoswimming.dto.response;

import java.util.List;

public record AdminCoachListResponse(
    List<AdminCoachListItemResponse> list, Long total, Integer page, Integer pageSize) {}
