package com.wordpress.kkaravitis.banking.account.domain.values;

import com.wordpress.kkaravitis.banking.account.domain.types.ReleaseReason;
import java.util.UUID;

public record ReleaseOutcome(UUID transferId, String reservationId, ReleaseReason reason) {

}
