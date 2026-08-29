package com.evplanner.station;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingConnectorRepository
        extends JpaRepository<ChargingConnector, Long> {
}