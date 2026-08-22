package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CoachListRequest(
    @Min(value = 1, message = "页码不能小于 1") Integer page,
    @Min(value = 1, message = "每页条数不能小于 1")
        @Max(value = 50, message = "每页条数不能超过 50")
        Integer pageSize,
    @Size(max = 50, message = "搜索关键词不能超过 50 个字符") String keyword,
    @Pattern(regexp = "rating|price|time", message = "排序字段必须是 rating、price 或 time")
        String sortBy,
    @Pattern(regexp = "asc|desc", message = "排序方向必须是 asc 或 desc") String sortOrder) {}
