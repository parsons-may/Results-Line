package com.resultsline.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests with test-only thresholds. These do not use the shipped
 * placeholder config, so they test the engine logic, not any medical values.
 */
class IntervalRuleEngineTest {

    private final IntervalRuleEngine engine = new IntervalRuleEngine(new IntervalRulesConfig(List.of(
            new IntervalRulesConfig.Tier(0, 10),
            new IntervalRulesConfig.Tier(5, 3))));

    @Test
    void lowRiskRecordReturnsTenYears() {
        assertThat(engine.followUpYears(new PolypRecord(1))).isEqualTo(10);
    }

    @Test
    void higherRiskRecordReturnsShorterFollowUp() {
        int years = engine.followUpYears(new PolypRecord(5));
        assertThat(years).isLessThan(10).isEqualTo(3);
    }

    @Test
    void boundaryJustBelowThresholdStaysLowRisk() {
        assertThat(engine.followUpYears(new PolypRecord(4))).isEqualTo(10);
    }

    @Test
    void zeroPolypsMatchesBaseTier() {
        assertThat(engine.followUpYears(new PolypRecord(0))).isEqualTo(10);
    }

    @Test
    void tierOrderInConfigDoesNotMatter() {
        IntervalRuleEngine reversed = new IntervalRuleEngine(new IntervalRulesConfig(List.of(
                new IntervalRulesConfig.Tier(5, 3),
                new IntervalRulesConfig.Tier(0, 10))));
        assertThat(reversed.followUpYears(new PolypRecord(7))).isEqualTo(3);
    }

    @Test
    void negativePolypCountIsRejected() {
        assertThatThrownBy(() -> new PolypRecord(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configWithoutBaseTierIsRejected() {
        IntervalRulesConfig noBase = new IntervalRulesConfig(List.of(new IntervalRulesConfig.Tier(5, 3)));
        assertThatThrownBy(() -> new IntervalRuleEngine(noBase)).isInstanceOf(IllegalArgumentException.class);
    }
}
