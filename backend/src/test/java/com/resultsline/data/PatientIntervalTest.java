package com.resultsline.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.rules.IntervalRuleEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Runs each synthetic record through the rule engine with the shipped (placeholder) config. */
@SpringBootTest
class PatientIntervalTest {

    @Autowired
    private IntervalRuleEngine engine;

    @Test
    void everyRecordGetsAnIntervalFromTheEngine() {
        assertThat(SyntheticDataLoader.loadPatients()).allSatisfy(p ->
                assertThat(engine.followUpYears(p.toPolypRecord())).isPositive());
    }

    @Test
    void recordsProduceBothTenYearAndShorterIntervals() {
        var years = SyntheticDataLoader.loadPatients().stream()
                .map(p -> engine.followUpYears(p.toPolypRecord()))
                .toList();
        assertThat(years).contains(10);
        assertThat(years).anyMatch(y -> y < 10);
    }

    @Test
    void zeroPolypRecordIsLowRiskAndEightPolypRecordIsShorter() {
        var patients = SyntheticDataLoader.loadPatients();
        var none = patients.stream().filter(p -> p.polypCount() == 0).findFirst().orElseThrow();
        var many = patients.stream().filter(p -> p.polypCount() >= 8).findFirst().orElseThrow();
        assertThat(engine.followUpYears(none.toPolypRecord()))
                .isGreaterThan(engine.followUpYears(many.toPolypRecord()));
    }
}
