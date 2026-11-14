package com.evplanner.api;

import com.evplanner.vehicle.VehicleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<VehicleResponse> list() {
        return vehicleRepository.findAll().stream()
                .map(v -> new VehicleResponse(
                        v.getId(),
                        v.getName(),
                        v.getRangeKm(),
                        v.getBatteryCapacityKwh()))
                .toList();
    }

    public record VehicleResponse(
            Long id,
            String name,
            BigDecimal rangeKm,
            BigDecimal batteryCapacityKwh) {}
}
