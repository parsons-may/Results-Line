package com.resultsline.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.data.CallerQuestion;
import com.resultsline.data.PatientRecord;
import com.resultsline.data.SyntheticDataLoader;
import com.resultsline.identity.CallSession;
import com.resultsline.identity.IdentityVerifier;
import com.resultsline.rules.IntervalRuleEngine;
import com.resultsline.rules.IntervalRulesConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The demo client is a keyword script, not an AI. These tests only check that its replies are always
 * well formed and grounded. They say nothing about how well Claude does.
 */
class DemoClaudeClientTest {

    private final List<PatientRecord> patients = SyntheticDataLoader.loadPatients();
    private final IdentityVerifier verifier = new IdentityVerifier(patients);
    private final IntervalRuleEngine rules = new IntervalRuleEngine(new IntervalRulesConfig(List.of(
            new IntervalRulesConfig.Tier(0, 10), new IntervalRulesConfig.Tier(5, 3))));
    private final ResultsAssistant assistant = new ResultsAssistant(new DemoClaudeClient(), rules);

    @Test
    void everyLabeledQuestionGetsAWellFormedGroundedReply() {
        for (CallerQuestion q : SyntheticDataLoader.loadQuestions()) {
            PatientRecord p = patients.stream().filter(x -> x.patientId().equals(q.patientId())).findFirst().orElseThrow();
            CallSession session = new CallSession(verifier);
            session.verify(p.fullName(), p.dateOfBirth());

            AssistantResult result = assistant.ask(session, q.text());

            assertThat(result.problems()).as("%s: %s", q.questionId(), q.text()).isEmpty();
        }
    }
}
