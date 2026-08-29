package com.evplanner.station;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "charging_connector")
public class ChargingConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charging_station_id", nullable = false)
    private ChargingStation chargingStation;

    @Column(name = "connector_type", nullable = false, length = 50)
    private String connectorType;

    @Column(name = "power_kw")
    private BigDecimal powerKw;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "available_count")
    private Integer availableCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ChargingConnector() {
        // JPA
    }

    public ChargingConnector(
            ChargingStation chargingStation,
            String connectorType,
            BigDecimal powerKw,
            Integer quantity,
            Integer availableCount) {

        this.chargingStation = chargingStation;
        this.connectorType = connectorType;
        this.powerKw = powerKw;
        this.quantity = quantity;
        this.availableCount = availableCount;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChargingStation getChargingStation() {
        return chargingStation;
    }

    public void setChargingStation(ChargingStation chargingStation) {
        this.chargingStation = chargingStation;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public BigDecimal getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(BigDecimal powerKw) {
        this.powerKw = powerKw;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(Integer availableCount) {
        this.availableCount = availableCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}