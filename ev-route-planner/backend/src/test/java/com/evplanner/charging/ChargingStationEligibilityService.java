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

        return station.getConnectors()
                .stream()
                .anyMatch(connector ->
                        isUsable(connector)
                                && connector.getConnectorType()
                                .equalsIgnoreCase(
                                        vehicle.getDefaultConnectorType()
                                )
                );
    }

    private boolean isUsable(ChargingConnector connector) {

        if (connector.getPowerKw() == null ||
                connector.getPowerKw().signum() <= 0) {
            return false;
        }

        Integer available = connector.getAvailableCount();

        return available == null || available > 0;
    }
}