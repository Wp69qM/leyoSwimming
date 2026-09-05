package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public record KnowledgeUploadRequest(
    @NotBlank(message = "标题不能为空") String title,
    @NotBlank(message = "分类不能为空")
        @Pattern(
            regexp = "^(safety|technique|emergency|other)$",
            message = "分类必须是 safety、technique、emergency 或 other")
        String category,
    @NotNull(message = "上传文件不能为空") MultipartFile file) {}
