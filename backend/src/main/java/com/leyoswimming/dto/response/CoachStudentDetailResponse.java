package com.leyoswimming.dto.response;

public record CoachStudentDetailResponse(
    Long studentUserId,
    UserProfile userProfile,
    CoachSlice coachSlice,
    Summary summary) {

  public record UserProfile(
      String avatarUrl,
      String name,
      String phoneMasked,
      Integer age,
      String gender,
      Boolean hasSwimBasis,
      String swimStrokes,
      String swimYears,
      String personalDesc,
      Boolean isMinor,
      String guardianName,
      String guardianPhoneMasked) {}

  public record CoachSlice(
      String learningStrokes,
      Integer swimLevel,
      String basics,
      String notes) {}

  public record Summary(
      Integer totalHours,
      Integer remainingHours,
      String lastClassDate) {}
}
