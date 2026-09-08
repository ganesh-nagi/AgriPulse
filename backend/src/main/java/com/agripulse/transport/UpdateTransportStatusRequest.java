package com.agripulse.transport;

import jakarta.validation.constraints.NotNull;

public class UpdateTransportStatusRequest {

  @NotNull private TransportStatus status;

  public TransportStatus getStatus() {
    return status;
  }

  public void setStatus(TransportStatus status) {
    this.status = status;
  }
}
