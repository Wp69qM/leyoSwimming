package com.leyoswimming.dto.response;

public record UserCancelCheckResponse(boolean canCancel, CancelChecks checks) {

  public record CancelChecks(
      boolean noActivePackage, boolean noPendingOrder, boolean noOngoingBooking) {}
}
