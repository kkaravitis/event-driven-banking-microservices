package com.wordpress.kkaravitis.banking.transfer.application.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga")
public class SagaEntity {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(name = "saga_state", nullable = false)
    private String sagaState;

    @Column(name = "saga_data", nullable = false, columnDefinition = "jsonb")
    private String sagaDataJson;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SagaEntity() { }

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

    public UUID getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getSagaState() {
        return sagaState;
    }

    public void setSagaState(String sagaState) {
        this.sagaState = sagaState;
    }

    public String getSagaDataJson() {
        return sagaDataJson;
    }

    public void setSagaDataJson(String sagaDataJson) {
        this.sagaDataJson = sagaDataJson;
    }
}