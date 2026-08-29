package com.evplanner.charging;

import com.evplanner.station.ChargingConnector;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import org.springframework.stereotype.Service;

@Service
public class ChargingStationEligibilityService {

    public boolean isEligible(
            Vehicle vehicle,
            ChargingStation station) {

        if (vehicle.getDefaultConnectorType() == null) {
            return false;
        }

        if (station.getConnectors() == null ||
                station.getConnectors().isEmpty()) {
            return false;
        }

        return station.getConnectors()
                .stream()
                .anyMatch(connector ->
                        isCompatible(vehicle, connector));
    }

    private boolean isCompatible(
            Vehicle vehicle,
            ChargingConnector connector) {

        if (connector.getConnectorType() == null ||
                !connector.getConnectorType()
                        .equalsIgnoreCase(
                                vehicle.getDefaultConnectorType())) {
            return false;
        }

        if (connector.getPowerKw() == null ||
                connector.getPowerKw().signum() <= 0) {
            return false;
        }

        Integer availableCount = connector.getAvailableCount();

        return availableCount == null || availableCount > 0;
    }
}