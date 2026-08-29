package com.evplanner.journey;

import java.util.List;

public record JourneyPlan(
        List<JourneyOption> options,
        JourneyOption recommendedOption
) {
}