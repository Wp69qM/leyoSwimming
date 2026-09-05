package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KnowledgeAddRequest(
    @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 个字符")
        String title,
    @NotBlank(message = "分类不能为空")
        @Pattern(
            regexp = "^(safety|technique|emergency|other)$",
            message = "分类必须是 safety、technique、emergency 或 other")
        String category,
    @NotBlank(message = "内容类型不能为空")
        @Pattern(regexp = "^(text|markdown)$", message = "内容类型必须是 text 或 markdown")
        String contentType,
    @NotBlank(message = "内容不能为空") String content) {}
