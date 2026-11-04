package com.resultsline.rules;

import java.util.Comparator;
import org.springframework.stereotype.Component;

/**
 * Decides the follow-up interval from a record using configured tiers.
 * Plain code, no AI: the assistant never decides an interval (hard rule 2).
 */
@Component
public class IntervalRuleEngine {

    private final IntervalRulesConfig config;

    public IntervalRuleEngine(IntervalRulesConfig config) {
        if (config == null || config.tiers() == null || config.tiers().isEmpty()) {
            throw new IllegalArgumentException("Interval rules need at least one tier");
        }
        boolean hasBaseTier = config.tiers().stream().anyMatch(t -> t.minPolyps() == 0);
        if (!hasBaseTier) {
            throw new IllegalArgumentException(
                    "Interval rules need a tier with minPolyps 0 so every record matches a rule");
        }
        this.config = config;
    }

    /** Returns the follow-up interval in years for the record. */
    public int followUpYears(PolypRecord record) {
        return config.tiers().stream()
                .filter(t -> t.minPolyps() <= record.polypCount())
                .max(Comparator.comparingInt(IntervalRulesConfig.Tier::minPolyps))
                .orElseThrow()
                .intervalYears();
    }
}
