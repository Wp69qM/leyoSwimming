package com.leyoswimming.dto.response;

import java.util.List;

public record AdminCoachApplicationListResponse(
    List<AdminCoachApplicationListItemResponse> list, long total, int page, int pageSize) {}
