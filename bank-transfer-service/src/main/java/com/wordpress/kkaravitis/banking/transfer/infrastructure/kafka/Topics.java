package com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record Topics(
      String transferExecutionSagaRepliesTopic,
      String transferCancellationSagaRepliesTopic,
      String accountsServiceCommandsTopic,
      String antiFraudServiceCommandsTopic
) {
}
