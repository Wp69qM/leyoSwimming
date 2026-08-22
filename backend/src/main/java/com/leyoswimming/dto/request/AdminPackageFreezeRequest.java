package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPackageFreezeRequest(
    @NotNull(message = "套餐 ID 不能为空")
    Long packageId,
    @NotBlank(message = "冻结原因不能为空")
    String reasonDetail,
    @NotNull(message = "版本号不能为空")
    Integer version) {}
