package com.leyoswimming.dto.response;

import java.util.List;

public record KnowledgeListResponse(
    List<KnowledgeListItemResponse> list,
    long total,
    int page,
    int pageSize) {}
