package com.resultsline.rules;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Follow-up interval rules, loaded from configuration (application.yml).
 * The doctor sets the real values; the code never hard-codes thresholds.
 *
 * @param tiers ordered or unordered list of tiers; the engine picks the tier with the
 *              highest minPolyps that is still less than or equal to the polyp count
 */
@ConfigurationProperties(prefix = "results-line.interval-rules")
public record IntervalRulesConfig(List<Tier> tiers) {

    /**
     * @param minPolyps     this tier applies when polyp count is at least this number
     * @param intervalYears recommended years until the next procedure
     */
    public record Tier(int minPolyps, int intervalYears) {
    }
}
