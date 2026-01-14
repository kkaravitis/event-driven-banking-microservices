package com.wordpress.kkaravitis.banking.antifraud.domain;

public record FraudDecision(boolean approved, String reason) {

    public static FraudDecision ok() {
        return new FraudDecision(true, null);
    }

    public static FraudDecision rejected(String reason) {
        return new FraudDecision(false, reason);
    }
}
