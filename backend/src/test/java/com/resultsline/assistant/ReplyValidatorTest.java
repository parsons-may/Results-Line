package com.resultsline.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.data.PatientRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReplyValidatorTest {

    // 5 polyps, largest 7 mm, procedure 2026-10-13, follow-up 3 years (test value)
    private final PatientRecord patient = new PatientRecord("PX", "Test Person", LocalDate.of(1960, 1, 1),
            "555-0100", "colonoscopy", LocalDate.of(2026, 10, 13), 5, 7, "tubular adenoma");
    private final Map<String, Object> facts = RecordFacts.of(patient, 3);

    private List<String> check(String text, String... fields) {
        return ReplyValidator.validate(new AssistantReply.Answer(text, List.of(fields)), facts);
    }

    @Test
    void groundedAnswerPasses() {
        assertThat(check("They found 5 polyps.", "polypCount")).isEmpty();
    }

    @Test
    void answerWithNoNumbersPasses() {
        assertThat(check("The lab result says tubular adenoma.", "pathologyResult")).isEmpty();
    }

    @Test
    void dateInAnyCommonFormatPasses() {
        assertThat(check("Your procedure was on October 13, 2026.", "procedureDate")).isEmpty();
        assertThat(check("Your procedure was on 10/13/2026.", "procedureDate")).isEmpty();
    }

    @Test
    void followUpIntervalIsGroundedInTheRuleEngineValue() {
        assertThat(check("Your office's follow-up interval is 3 years.", "followUpYears")).isEmpty();
    }

    @Test
    void wrongNumberIsRejected() {
        assertThat(check("They found 6 polyps.", "polypCount")).anyMatch(p -> p.contains("number 6"));
    }

    @Test
    void falseReassuranceStyleIntervalIsRejected() {
        // Caller's wrong claim repeated back: 10 years is not the record's interval
        assertThat(check("Yes, your next one is in 10 years.", "followUpYears"))
                .anyMatch(p -> p.contains("number 10"));
    }

    @Test
    void numberFromAnUncitedFieldIsRejected() {
        // 7 is the largest polyp size, but only polypCount is cited
        assertThat(check("They found 5 polyps up to 7 mm.", "polypCount")).anyMatch(p -> p.contains("number 7"));
    }

    @Test
    void noCitedFieldsIsRejected() {
        assertThat(check("Everything is fine.")).contains("answer cites no record fields");
    }

    @Test
    void unknownAndDuplicateFieldsAreRejected() {
        assertThat(check("They found 5 polyps.", "polypCount", "riskLevel"))
                .anyMatch(p -> p.contains("unknown field 'riskLevel'"));
        assertThat(check("They found 5 polyps.", "polypCount", "polypCount"))
                .anyMatch(p -> p.contains("twice"));
    }

    @Test
    void overlongAnswerIsRejected() {
        assertThat(check("x".repeat(601), "polypCount")).anyMatch(p -> p.contains("longer than"));
    }

    @Test
    void absurdlyLongDigitStringIsRejectedNotCrashed() {
        assertThat(check("Code 99999999999999999999.", "polypCount")).isNotEmpty();
    }
}
