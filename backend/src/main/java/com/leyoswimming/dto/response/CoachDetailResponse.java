package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachDetailResponse(
    Long id,
    String name,
    String avatar,
    Integer gender,
    Integer age,
    Integer status,
    BigDecimal rating,
    Integer yearsOfTeaching,
    Integer totalStudents,
    Integer totalHours,
    List<String> teachingStrokes,
    String bio,
    BigDecimal referencePrice,
    CoachDetailContactResponse contact,
    List<CoachDetailCertificateResponse> certificates,
    List<CoachDetailPackageResponse> packages,
    List<CoachDetailReviewResponse> reviews,
    List<CoachDetailAvailableTimeResponse> availableTimes,
    String realTimeStatus) {}
