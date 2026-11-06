package com.resultsline.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.data.PatientRecord;
import com.resultsline.data.SyntheticDataLoader;
import com.resultsline.identity.CallSession;
import com.resultsline.identity.IdentityVerifier;
import com.resultsline.rules.IntervalRuleEngine;
import com.resultsline.rules.IntervalRulesConfig;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Uses a fake Claude, so no API key is needed and nothing is sent anywhere. */
class ResultsAssistantTest {

    private final List<PatientRecord> patients = SyntheticDataLoader.loadPatients();
    private final IdentityVerifier verifier = new IdentityVerifier(patients);
    private final IntervalRuleEngine rules = new IntervalRuleEngine(new IntervalRulesConfig(List.of(
            new IntervalRulesConfig.Tier(0, 10), new IntervalRulesConfig.Tier(5, 3))));

    private CallSession verifiedSessionForP006() {
        CallSession s = new CallSession(verifier);
        s.verify("Finley Placeholder", LocalDate.of(1963, 9, 9)); // P006: 5 polyps
        return s;
    }

    private ResultsAssistant assistantReplying(String reply, List<String> promptsSeen) {
        return new ResultsAssistant((system, user) -> {
            promptsSeen.add(system + "\n" + user);
            return reply;
        }, rules);
    }

    @Test
    void unverifiedCallerGetsHandoffAndClaudeIsNeverCalled() {
        List<String> seen = new ArrayList<>();
        ResultsAssistant assistant = assistantReplying("{\"type\":\"handoff\",\"reason\":\"x\"}", seen);

        AssistantResult result = assistant.ask(new CallSession(verifier), "How many polyps did I have?");

        assertThat(result.reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(result.problems()).containsExactly("caller not verified");
        assertThat(seen).isEmpty();
    }

    @Test
    void lockedCallerGetsHandoffAndClaudeIsNeverCalled() {
        List<String> seen = new ArrayList<>();
        ResultsAssistant assistant = assistantReplying("{\"type\":\"handoff\",\"reason\":\"x\"}", seen);
        CallSession session = new CallSession(verifier);
        for (int i = 0; i < 3; i++) {
            session.verify("Nobody Here", LocalDate.of(2000, 1, 1));
        }
        session.verify("Finley Placeholder", LocalDate.of(1963, 9, 9));

        assertThat(assistant.ask(session, "How many polyps?").reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(seen).isEmpty();
    }

    @Test
    void validAnswerPassesThrough() {
        List<String> seen = new ArrayList<>();
        ResultsAssistant assistant = assistantReplying(
                "{\"type\":\"answer\",\"answer\":\"They found 5 polyps.\",\"fields_used\":[\"polypCount\"]}", seen);

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "How many polyps did I have?");

        assertThat(result.problems()).isEmpty();
        assertThat(result.reply()).isEqualTo(new AssistantReply.Answer("They found 5 polyps.", List.of("polypCount")));
    }

    @Test
    void validHandoffPassesThrough() {
        ResultsAssistant assistant = assistantReplying(
                "{\"type\":\"handoff\",\"reason\":\"medical advice\"}", new ArrayList<>());

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "Should I change my diet?");

        assertThat(result.problems()).isEmpty();
        assertThat(result.reply()).isEqualTo(new AssistantReply.Handoff("medical advice"));
    }

    @Test
    void malformedReplyBecomesHandoffWithProblemLogged() {
        ResultsAssistant assistant = assistantReplying("You're completely fine!", new ArrayList<>());

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "Am I fine?");

        assertThat(result.reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(result.problems()).singleElement().asString().startsWith("bad reply structure");
    }

    @Test
    void ungroundedNumberBecomesHandoff() {
        ResultsAssistant assistant = assistantReplying(
                "{\"type\":\"answer\",\"answer\":\"Your next one is in 10 years.\",\"fields_used\":[\"followUpYears\"]}",
                new ArrayList<>());

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "Confirm 10 years?");

        assertThat(result.reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(result.problems()).anyMatch(p -> p.contains("number 10"));
    }

    @Test
    void answerCitingUnknownFieldBecomesHandoff() {
        ResultsAssistant assistant = assistantReplying(
                "{\"type\":\"answer\",\"answer\":\"Low risk.\",\"fields_used\":[\"riskLevel\"]}", new ArrayList<>());

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "Is it low risk?");

        assertThat(result.reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(result.problems()).anyMatch(p -> p.contains("unknown field"));
    }

    @Test
    void claudeFailureBecomesHandoff() {
        ResultsAssistant assistant = new ResultsAssistant((system, user) -> {
            throw new IllegalStateException("network down");
        }, rules);

        AssistantResult result = assistant.ask(verifiedSessionForP006(), "How many polyps?");

        assertThat(result.reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(result.problems()).singleElement().asString().contains("network down");
    }

    @Test
    void blankQuestionBecomesHandoffWithoutCallingClaude() {
        List<String> seen = new ArrayList<>();
        ResultsAssistant assistant = assistantReplying("{\"type\":\"handoff\",\"reason\":\"x\"}", seen);

        assertThat(assistant.ask(verifiedSessionForP006(), "  ").reply()).isInstanceOf(AssistantReply.Handoff.class);
        assertThat(seen).isEmpty();
    }

    @Test
    void promptContainsOnlyTheVerifiedPatientsFactsAndNoIdentityDetails() {
        List<String> seen = new ArrayList<>();
        ResultsAssistant assistant = assistantReplying("{\"type\":\"handoff\",\"reason\":\"x\"}", seen);

        assistant.ask(verifiedSessionForP006(), "How many polyps did I have?");

        assertThat(seen).hasSize(1);
        String prompt = seen.get(0);
        // The caller's own identity details are not sent: they are not needed to answer.
        assertThat(prompt).doesNotContain("Finley").doesNotContain("Placeholder")
                .doesNotContain("1963-09-09").doesNotContain("555-01");
        // No other patient's details are sent.
        for (PatientRecord other : patients) {
            assertThat(prompt).doesNotContain(other.fullName()).doesNotContain(other.patientId());
        }
        // The verified patient's record facts and the question are sent.
        assertThat(prompt).contains("\"polypCount\" : 5").contains("\"followUpYears\" : 3")
                .contains("How many polyps did I have?");
    }
}
