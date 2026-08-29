package com.evplanner.vehicle;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "battery_capacity_kwh", precision = 8, scale = 2)
    private BigDecimal batteryCapacityKwh;

    @Column(name = "usable_battery_capacity_kwh", precision = 8, scale = 2)
    private BigDecimal usableBatteryCapacityKwh;

    @Column(name = "range_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal rangeKm;

    @Column(name = "default_connector_type", length = 50)
    private String defaultConnectorType;

    @Column(name = "consumption_kwh_per_100km", nullable = false, precision = 8, scale = 2)
    private BigDecimal consumptionKwhPer100Km;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Vehicle() {
        // JPA
    }

    public Vehicle(
            String name,
            BigDecimal batteryCapacityKwh,
            BigDecimal usableBatteryCapacityKwh,
            BigDecimal rangeKm,
            BigDecimal consumptionKwhPer100Km,
            String defaultConnectorType) {

        this.name = name;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.usableBatteryCapacityKwh = usableBatteryCapacityKwh;
        this.rangeKm = rangeKm;
        this.consumptionKwhPer100Km = consumptionKwhPer100Km;
        this.defaultConnectorType = defaultConnectorType;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBatteryCapacityKwh() {
        return batteryCapacityKwh;
    }

    public void setBatteryCapacityKwh(BigDecimal batteryCapacityKwh) {
        this.batteryCapacityKwh = batteryCapacityKwh;
    }

    public BigDecimal getUsableBatteryCapacityKwh() {
        return usableBatteryCapacityKwh;
    }

    public void setUsableBatteryCapacityKwh(BigDecimal usableBatteryCapacityKwh) {
        this.usableBatteryCapacityKwh = usableBatteryCapacityKwh;
    }

    public BigDecimal getRangeKm() {
        return rangeKm;
    }

    public void setRangeKm(BigDecimal rangeKm) {
        this.rangeKm = rangeKm;
    }

    public BigDecimal getConsumptionKwhPer100Km() {
        return consumptionKwhPer100Km;
    }

    public void setConsumptionKwhPer100Km(BigDecimal consumptionKwhPer100Km) {
        this.consumptionKwhPer100Km = consumptionKwhPer100Km;
    }

    public String getDefaultConnectorType() {
        return defaultConnectorType;
    }

    public void setDefaultConnectorType(String defaultConnectorType) {
        this.defaultConnectorType = defaultConnectorType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}