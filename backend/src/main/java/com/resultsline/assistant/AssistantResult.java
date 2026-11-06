package com.resultsline.assistant;

import java.util.List;

/**
 * What the assistant returns for one question.
 *
 * @param reply    what to tell the caller: a validated answer, or a handoff
 * @param problems empty when Claude's own reply was used. Non-empty when the code forced a handoff
 *                 (caller not verified, bad structure, failed validation, or a failed call).
 *                 The evaluation harness logs these.
 */
public record AssistantResult(AssistantReply reply, List<String> problems) {

    static AssistantResult accepted(AssistantReply reply) {
        return new AssistantResult(reply, List.of());
    }

    static AssistantResult forcedHandoff(String reason, List<String> problems) {
        return new AssistantResult(new AssistantReply.Handoff(reason), List.copyOf(problems));
    }
}
