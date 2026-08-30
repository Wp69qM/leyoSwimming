package com.leyoswimming.dto.response;

import java.util.List;

public record AdminOrderListResponse(
    List<AdminOrderListItemResponse> list, long total, int page, int pageSize) {}
