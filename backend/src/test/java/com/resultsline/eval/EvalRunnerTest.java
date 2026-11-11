package com.resultsline.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.assistant.ClaudeClient;
import com.resultsline.assistant.ResultsAssistant;
import com.resultsline.assistant.TokenUsage;
import com.resultsline.data.CallerQuestion;
import com.resultsline.data.ExpectedBehavior;
import com.resultsline.data.PatientRecord;
import com.resultsline.data.SyntheticDataLoader;
import com.resultsline.identity.IdentityVerifier;
import com.resultsline.rules.IntervalRuleEngine;
import com.resultsline.rules.IntervalRulesConfig;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests the scoring and reporting with scripted fake Claudes. These say nothing about the real Claude:
 * they prove the harness counts each kind of failure correctly.
 */
class EvalRunnerTest {

    private final List<PatientRecord> patients = SyntheticDataLoader.loadPatients();
    private final List<CallerQuestion> questions = SyntheticDataLoader.loadQuestions();
    private final IdentityVerifier verifier = new IdentityVerifier(patients);
    private final IntervalRuleEngine rules = new IntervalRuleEngine(new IntervalRulesConfig(List.of(
            new IntervalRulesConfig.Tier(0, 10), new IntervalRulesConfig.Tier(5, 3))));
    private final Map<String, CallerQuestion> byText = questions.stream()
            .collect(Collectors.toMap(CallerQuestion::text, Function.identity()));

    private List<EvalRecord> run(ClaudeClient fake) {
        return new EvalRunner(new ResultsAssistant(fake, rules), verifier).run(patients, questions);
    }

    /** Pulls the caller's question back out of the prompt so a fake can respond per question. */
    private CallerQuestion questionIn(String userMessage) {
        int start = userMessage.indexOf("<question>\n") + "<question>\n".length();
        int end = userMessage.lastIndexOf("\n</question>");
        return byText.get(userMessage.substring(start, end));
    }

    private static String answerJson(String text, List<String> fields) {
        String list = fields.stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","));
        return "{\"type\":\"answer\",\"answer\":\"" + text + "\",\"fields_used\":[" + list + "]}";
    }

    private static final String HANDOFF = "{\"type\":\"handoff\",\"reason\":\"staff\"}";

    @Test
    void aPerfectAssistantScoresCleanly() {
        List<EvalRecord> records = run((system, user) -> {
            CallerQuestion q = questionIn(user);
            return q.expected() == ExpectedBehavior.ANSWER
                    ? answerJson("Here is your information.", q.requiredFields())
                    : HANDOFF;
        });
        EvalSummary s = EvalSummary.of(records);

        assertThat(s.total()).isEqualTo(questions.size());
        assertThat(s.correctAnswers()).isEqualTo(s.shouldAnswer());
        assertThat(s.correctRefusals()).isEqualTo(s.shouldRefuse());
        assertThat(s.falseReassurance()).isZero();
        assertThat(s.overRefusals()).isZero();
        assertThat(s.forcedHandoffs()).isZero();
    }

    @Test
    void anAssistantThatAlwaysHandsOffShowsOverRefusalButNoFalseReassurance() {
        EvalSummary s = EvalSummary.of(run((system, user) -> HANDOFF));

        assertThat(s.overRefusals()).isEqualTo(s.shouldAnswer());
        assertThat(s.correctRefusals()).isEqualTo(s.shouldRefuse());
        assertThat(s.correctAnswers()).isZero();
        assertThat(s.falseReassurance()).isZero();
    }

    @Test
    void anAssistantThatAlwaysReassuresIsCountedOnTheFalseReassuranceLine() {
        EvalSummary s = EvalSummary.of(run((system, user) ->
                answerJson("You are fine.", List.of("polypCount"))));

        assertThat(s.falseReassurance()).isEqualTo(s.total());
        assertThat(s.falseReassuranceOnTraps()).isEqualTo(s.traps());
        assertThat(s.correctAnswers()).isZero();
    }

    @Test
    void anAssistantThatAnswersWithoutReassuringStillFailsTheTraps() {
        // Plain fact, no reassuring words: still wrong for questions that should be handed off.
        EvalSummary s = EvalSummary.of(run((system, user) ->
                answerJson("Your record has polyp information.", List.of("polypCount"))));

        assertThat(s.falseReassuranceOnTraps()).isEqualTo(
                (int) questions.stream().filter(q -> q.falseReassuranceTrap()
                        && q.expected() == ExpectedBehavior.REFUSE).count());
        assertThat(s.answeredWhenShouldRefuse()).isPositive();
    }

    @Test
    void badRepliesAreCaughtByCodeAndCountedAsForcedHandoffs() {
        EvalSummary s = EvalSummary.of(run((system, user) -> "You're all good!"));

        assertThat(s.forcedHandoffs()).isEqualTo(s.total());
        assertThat(s.falseReassurance()).isZero(); // the caller never sees the bad reply
        assertThat(s.overRefusals()).isEqualTo(s.shouldAnswer());
    }

    @Test
    void failedCallsAreDetected() {
        List<EvalRecord> records = run((system, user) -> {
            throw new IllegalStateException("401 unauthorized");
        });
        assertThat(records).allMatch(EvalRecord::claudeCallFailed);
    }

    @Test
    void everyQuestionGetsExactlyOneOutcomeAndCountsAddUp() {
        EvalSummary s = EvalSummary.of(run((system, user) -> HANDOFF));
        int sum = s.correctAnswers() + s.incompleteAnswers() + s.overRefusals() + s.correctRefusals()
                + s.answeredWhenShouldRefuse() + s.falseReassurance();
        assertThat(sum).isEqualTo(s.total());
    }

    @Test
    void reportLabelsTheClientAndShowsFalseReassuranceOnItsOwnLine() {
        List<EvalRecord> records = run((system, user) -> answerJson("You are fine.", List.of("polypCount")));
        String md = EvalReportWriter.markdown("DEMO CLIENT, NOT CLAUDE", "2026-11-11", records,
                new TokenUsage(0, 0));

        assertThat(md).contains("DEMO CLIENT, NOT CLAUDE")
                .contains("## False reassurance (reported on its own, never averaged)")
                .contains("**45 of 45 questions** got a reassuring answer")
                .contains("## Every failure")
                .contains("| Q019 |");
        assertThat(md).doesNotContain("Tokens used");
    }

    @Test
    void reportListsTokensWhenKnownAndSaysNoneWhenNoFailures() {
        List<EvalRecord> records = run((system, user) -> {
            CallerQuestion q = questionIn(user);
            return q.expected() == ExpectedBehavior.ANSWER
                    ? answerJson("Here is your information.", q.requiredFields())
                    : HANDOFF;
        });
        String md = EvalReportWriter.markdown("Claude (test)", "2026-11-11", records, new TokenUsage(1200, 3400));

        assertThat(md).contains("1200 input, 3400 output").contains("## Every failure\n\nNone.");
    }
}
