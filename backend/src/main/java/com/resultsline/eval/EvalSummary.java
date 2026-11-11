package com.resultsline.eval;

import com.resultsline.data.ExpectedBehavior;
import java.util.List;

/** Counts for the report. Each measure has its own denominator; nothing is averaged together. */
public record EvalSummary(
        int total,
        int shouldAnswer,
        int shouldRefuse,
        int traps,
        int correctAnswers,
        int incompleteAnswers,
        int overRefusals,
        int correctRefusals,
        int answeredWhenShouldRefuse,
        int falseReassurance,
        int falseReassuranceOnTraps,
        int forcedHandoffs) {

    public static EvalSummary of(List<EvalRecord> records) {
        return new EvalSummary(
                records.size(),
                count(records, r -> r.question().expected() == ExpectedBehavior.ANSWER),
                count(records, r -> r.question().expected() == ExpectedBehavior.REFUSE),
                count(records, r -> r.question().falseReassuranceTrap()),
                count(records, r -> r.outcome() == EvalOutcome.CORRECT_ANSWER),
                count(records, r -> r.outcome() == EvalOutcome.INCOMPLETE_ANSWER),
                count(records, r -> r.outcome() == EvalOutcome.OVER_REFUSAL),
                count(records, r -> r.outcome() == EvalOutcome.CORRECT_REFUSAL),
                count(records, r -> r.outcome() == EvalOutcome.ANSWERED_WHEN_SHOULD_REFUSE),
                count(records, r -> r.outcome() == EvalOutcome.FALSE_REASSURANCE),
                count(records, r -> r.outcome() == EvalOutcome.FALSE_REASSURANCE && r.question().falseReassuranceTrap()),
                count(records, EvalRecord::forcedHandoff));
    }

    private static int count(List<EvalRecord> records, java.util.function.Predicate<EvalRecord> test) {
        return (int) records.stream().filter(test).count();
    }
}
