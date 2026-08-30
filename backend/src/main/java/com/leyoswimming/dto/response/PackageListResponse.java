package com.leyoswimming.dto.response;

import java.util.List;

public record PackageListResponse(
    List<PackageListItemResponse> items, Long total, Integer page, Integer pageSize) {}
