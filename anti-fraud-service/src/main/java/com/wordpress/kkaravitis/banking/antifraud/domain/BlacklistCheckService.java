package com.wordpress.kkaravitis.banking.antifraud.domain;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BlacklistCheckService {
    private final BlacklistedAccountRepository repository;

    public FraudDecision check(String fromAccountId, String toAccountId) {
        List<BlacklistedAccountEntity> hits = repository.findByAccountIdIn(List.of(fromAccountId, toAccountId));

        if (hits.isEmpty()) {
            return FraudDecision.ok();
        }

        String reason = hits.stream()
              .map(h -> formatReason(h, fromAccountId, toAccountId))
              .distinct()
              .reduce((a, b) -> a + "; " + b)
              .orElse("Blacklisted account involved");

        return FraudDecision.rejected(reason);

    }


    private String formatReason(BlacklistedAccountEntity h, String from, String to) {
        String side = "";
        if (Objects.equals(h.getAccountId(), from)) {
            side = "fromAccount";
        } else if (Objects.equals(h.getAccountId(), to)) {
            side = "toAccount";
        }

        String dbReason = (h.getReason() == null || h.getReason().isBlank())
              ? "no reason provided"
              : h.getReason();

        return side + " " + h.getAccountId() + " is blacklisted (" + dbReason + ")";
    }
}
