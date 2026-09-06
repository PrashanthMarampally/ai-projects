package com.evplanner.api;

import com.evplanner.intelligence.JourneyIntelligenceEngine;
import com.evplanner.journey.GeoPoint;
import com.evplanner.journey.ChargingStopPlan;
import com.evplanner.journey.JourneyOption;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JourneyControllerTest {

    @Test
    void shouldPlanJourney() throws Exception {
        JourneyIntelligenceEngine engine = mock(JourneyIntelligenceEngine.class);

        ChargingStopPlan stop = new ChargingStopPlan(
                101L,
                "ABC EV Station",
                BigDecimal.valueOf(142.30),
                BigDecimal.valueOf(399.10),
                BigDecimal.valueOf(17.80),
                BigDecimal.valueOf(65.20),
                BigDecimal.valueOf(28.40),
                BigDecimal.valueOf(60),
                29);

        JourneyOption option = new JourneyOption(
                BigDecimal.valueOf(320),
                240,
                List.of(stop),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(80)
        );
        JourneyPlan plan = new JourneyPlan(List.of(option), option);
        when(engine.plan(any(JourneyRequest.class))).thenReturn(plan);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new JourneyController(engine))
                .setControllerAdvice(new JourneyApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/journeys/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": {
                                    "latitude": 17.3850,
                                    "longitude": 78.4867
                                  },
                                  "destination": {
                                    "latitude": 12.9716,
                                    "longitude": 77.5946
                                  },
                                  "vehicleId": 1,
                                  "currentSocPercent": 65,
                                  "minimumArrivalSocPercent": 15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recommendedOption.distanceKm").value(320))
                .andExpect(jsonPath("$.recommendedOption.estimatedDurationMinutes").value(240))
                .andExpect(jsonPath("$.recommendedOption.chargingStops[0].stationId").value(101))
                .andExpect(jsonPath("$.recommendedOption.chargingStops[0].stationName").value("ABC EV Station"))
                .andExpect(jsonPath("$.recommendedOption.chargingStops[0].energyToAddKwh").value(28.4))
                .andExpect(jsonPath("$.recommendedOption.chargingStops[0].chargingPowerKw").value(60))
                .andExpect(jsonPath("$.recommendedOption.chargingStops[0].estimatedChargingMinutes").value(29));

        verify(engine).plan(any(JourneyRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenEngineRejectsRequest() throws Exception {
        JourneyIntelligenceEngine engine = mock(JourneyIntelligenceEngine.class);
        when(engine.plan(any(JourneyRequest.class)))
                .thenThrow(new IllegalArgumentException("Current SOC must be between 0 and 100"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new JourneyController(engine))
                .setControllerAdvice(new JourneyApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/journeys/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": {
                                    "latitude": 17.3850,
                                    "longitude": 78.4867
                                  },
                                  "destination": {
                                    "latitude": 12.9716,
                                    "longitude": 77.5946
                                  },
                                  "vehicleId": 1,
                                  "currentSocPercent": 120,
                                  "minimumArrivalSocPercent": 15
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid journey request"));
    }

    @Test
    void shouldReturnNotFoundWhenVehicleDoesNotExist() throws Exception {
        JourneyIntelligenceEngine engine = mock(JourneyIntelligenceEngine.class);
        when(engine.plan(any(JourneyRequest.class)))
                .thenThrow(new IllegalArgumentException("Vehicle not found: 999"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new JourneyController(engine))
                .setControllerAdvice(new JourneyApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/journeys/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": {
                                    "latitude": 17.3850,
                                    "longitude": 78.4867
                                  },
                                  "destination": {
                                    "latitude": 12.9716,
                                    "longitude": 77.5946
                                  },
                                  "vehicleId": 999,
                                  "currentSocPercent": 65,
                                  "minimumArrivalSocPercent": 15
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Vehicle not found"));
    }
}
