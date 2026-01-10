package com.wordpress.kkaravitis.banking.common;

public interface BankingEventType {
    String getMessageType();

    Class<?> getPayloadType();
}
