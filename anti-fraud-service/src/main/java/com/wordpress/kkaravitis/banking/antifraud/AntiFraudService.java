package com.wordpress.kkaravitis.banking.antifraud;

import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import lombok.Builder;
import lombok.Getter;

public interface AntiFraudService {

    void handleCheckFraudCommand(CheckFraudCommandContext context);

    @Getter
    @Builder
    class CheckFraudCommandContext {
        private final String replyTopic;
        private final String correlationId;
        private final CheckFraudCommand checkFraudCommand;
    }
}
