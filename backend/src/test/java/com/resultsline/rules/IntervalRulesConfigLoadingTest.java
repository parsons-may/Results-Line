package com.resultsline.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Checks that the shipped application.yml loads and drives the engine (placeholder values). */
@SpringBootTest
class IntervalRulesConfigLoadingTest {

    @Autowired
    private IntervalRuleEngine engine;

    @Test
    void shippedConfigGivesTenYearsForLowRisk() {
        assertThat(engine.followUpYears(new PolypRecord(1))).isEqualTo(10);
    }

    @Test
    void shippedConfigGivesShorterFollowUpForFiveOrMorePolyps() {
        assertThat(engine.followUpYears(new PolypRecord(5))).isLessThan(10);
    }
}
