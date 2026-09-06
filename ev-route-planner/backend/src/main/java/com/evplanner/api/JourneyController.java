package com.evplanner.api;

import com.evplanner.intelligence.JourneyIntelligenceEngine;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyIntelligenceEngine journeyIntelligenceEngine;

    public JourneyController(JourneyIntelligenceEngine journeyIntelligenceEngine) {
        this.journeyIntelligenceEngine = journeyIntelligenceEngine;
    }

    @PostMapping("/plan")
    public ResponseEntity<JourneyPlan> plan(@RequestBody JourneyRequest request) {
        return ResponseEntity.ok(journeyIntelligenceEngine.plan(request));
    }
}
