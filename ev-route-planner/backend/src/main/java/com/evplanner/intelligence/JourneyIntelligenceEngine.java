package com.evplanner.intelligence;

import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;

public interface JourneyIntelligenceEngine {

    JourneyPlan plan(JourneyRequest request);
}