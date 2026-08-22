package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AdminPackageExtendRequest(
    @NotNull(message = "套餐 ID 不能为空")
    Long packageId,
    @NotNull(message = "新到期时间不能为空")
    LocalDateTime newExpireAt,
    @NotBlank(message = "延期原因不能为空")
    @Size(max = 200, message = "延期原因不超过 200 字")
    String reason,
    @NotNull(message = "版本号不能为空")
    Integer version) {}
