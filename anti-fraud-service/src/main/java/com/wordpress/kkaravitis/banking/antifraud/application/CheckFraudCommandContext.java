package com.wordpress.kkaravitis.banking.antifraud.application;

import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckFraudCommandContext {
    private final String messageId;
    private final UUID correlationId;
    private final CheckFraudCommand checkFraudCommand;
    private final String destinationTopic;

}
