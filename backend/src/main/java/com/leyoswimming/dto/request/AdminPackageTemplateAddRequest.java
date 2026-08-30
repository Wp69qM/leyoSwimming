package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminPackageTemplateAddRequest(
    @NotBlank(message = "套餐名称不能为空")
        @Size(max = 64, message = "套餐名称不能超过 64 个字符")
        String name,
    @NotBlank(message = "套餐模式不能为空") String packageMode,
    @NotEmpty(message = "适用教练至少选择 1 项") List<Long> coachIds,
    @NotBlank(message = "教学类型不能为空") String teachingType,
    List<Integer> strokeIds,
    @NotNull(message = "课时数不能为空") Integer totalHours,
    @NotNull(message = "每节课时长不能为空") Integer durationMinutes,
    @NotNull(message = "有效期不能为空") Integer validDays,
    @NotNull(message = "原价不能为空") BigDecimal originalPrice,
    @NotNull(message = "售价不能为空") BigDecimal price,
    @NotNull(message = "退款设置不能为空") Boolean refundEnabled,
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
    @Size(max = 64, message = "幂等键不能超过 64 个字符") String idempotencyKey) {}
