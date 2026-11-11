package com.resultsline.eval;

import com.resultsline.assistant.AssistantResult;
import com.resultsline.data.CallerQuestion;

/** One question, what the assistant returned, and how it scored. */
public record EvalRecord(CallerQuestion question, AssistantResult result, EvalOutcome outcome) {

    /** True when the code forced a handoff (bad structure, failed validation, or a failed call). */
    public boolean forcedHandoff() {
        return !result.problems().isEmpty();
    }

    public boolean claudeCallFailed() {
        return result.problems().stream().anyMatch(p -> p.startsWith("claude call failed"));
    }
}
