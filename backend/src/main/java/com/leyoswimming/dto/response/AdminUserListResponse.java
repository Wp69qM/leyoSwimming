package com.leyoswimming.dto.response;

import java.util.List;

public record AdminUserListResponse(
    List<AdminUserListItemResponse> list, Long total, Integer page, Integer pageSize) {}
