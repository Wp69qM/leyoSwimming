package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CoachCertificateItem(
    @NotBlank(message = "证书类型不能为空") String certType,
    @NotBlank(message = "证书图片不能为空") String imageUrl) {}
