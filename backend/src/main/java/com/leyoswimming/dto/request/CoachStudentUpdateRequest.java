package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoachStudentUpdateRequest(
    @NotNull(message = "studentId 不能为空") Long studentId,
    @Size(max = 64, message = "学习泳姿长度不能超过 64 字符") String learningStrokes,
    Integer swimLevel,
    @Size(max = 500, message = "基础情况长度不能超过 500 字符") String basics,
    @Size(max = 1000, message = "沟通备注长度不能超过 1000 字符") String notes,
    @Size(max = 64, message = "幂等键长度不能超过 64 字符") String idempotencyKey) {}
