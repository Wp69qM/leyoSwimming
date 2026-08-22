package com.leyoswimming.dto.response;

import java.util.List;

public record AdminPackageListResponse(
    List<AdminPackageListItemResponse> list, long total, int page, int pageSize) {}
