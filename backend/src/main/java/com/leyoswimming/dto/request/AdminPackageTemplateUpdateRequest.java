package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminPackageTemplateUpdateRequest(
    @NotNull(message = "套餐模板 ID 不能为空") Long packageTemplateId,
    @Size(max = 64, message = "套餐名称不能超过 64 个字符") String name,
    String packageMode,
    @NotEmpty(message = "适用教练至少选择 1 项") List<Long> coachIds,
    String teachingType,
    List<Integer> strokeIds,
    Integer totalHours,
    Integer durationMinutes,
    Integer validDays,
    BigDecimal originalPrice,
    BigDecimal price,
    Boolean refundEnabled,
    BigDecimal refundRatio,
    Integer refundValidDays,
    @Size(max = 20, message = "标签最多 20 个") List<String> tags,
    @Size(max = 500, message = "套餐描述不能超过 500 个字符") String description,
    @Size(max = 10, message = "图片最多 10 张")
        List<
                @Size(max = 512, message = "图片 URL 不能超过 512 个字符")
                @Pattern(
                    regexp = "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$",
                    message = "图片 URL 格式不正确")
                String>
            images,
    @NotNull(message = "版本号不能为空") Integer version) {}
