package com.wordpress.kkaravitis.banking.transfer.application.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "saga")
@EqualsAndHashCode(of = "sagaId")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaEntity {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(name = "saga_state", nullable = false)
    private String sagaState;

    @Column(name = "saga_data", nullable = false)
    private String sagaDataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SagaEntity(UUID sagaId,
          String sagaType,
          String sagaState,
          String sagaDataJson) {
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.sagaState = sagaState;
        this.sagaDataJson = sagaDataJson;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String sagaState, String sagaDataJson) {
        this.sagaState = sagaState;
        this.sagaDataJson = sagaDataJson;
    }
}