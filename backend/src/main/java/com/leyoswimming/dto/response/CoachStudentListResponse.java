package com.leyoswimming.dto.response;

import java.util.List;

public record CoachStudentListResponse(List<StudentItem> students) {

  public record StudentItem(
      Long studentUserId,
      String avatarUrl,
      String name,
      String gender,
      Integer age,
      Boolean isMinor,
      List<PackageTag> activePackageTags) {}

  public record PackageTag(String label, String type) {}
}
