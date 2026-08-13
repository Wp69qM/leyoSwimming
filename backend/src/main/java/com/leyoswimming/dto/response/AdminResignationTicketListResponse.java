package com.leyoswimming.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminResignationTicketListResponse(
    List<TicketItem> items, long total, int page, int pageSize) {

  public record TicketItem(
      Long ticketId,
      String ticketNo,
      Long coachId,
      String coachName,
      String coachPhone,
      String reason,
      String status,
      Integer totalPackages,
      Integer handledPackages,
      LocalDateTime submittedAt,
      LocalDateTime createdAt,
      LocalDateTime coachJoinedAt) {}
}
