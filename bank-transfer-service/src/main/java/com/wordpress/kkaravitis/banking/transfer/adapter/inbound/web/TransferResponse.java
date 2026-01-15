package com.wordpress.kkaravitis.banking.transfer.adapter.inbound.web;

public record TransferResponse(
      String transferId,
      String state,
      String message
) {}
