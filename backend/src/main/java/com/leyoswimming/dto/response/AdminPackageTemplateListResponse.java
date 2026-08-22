package com.leyoswimming.dto.response;

import java.util.List;

public record AdminPackageTemplateListResponse(
    List<AdminPackageTemplateListItemResponse> items,
    long total,
    int page,
    int pageSize) {}
