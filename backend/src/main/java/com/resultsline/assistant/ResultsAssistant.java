package com.resultsline.assistant;

import com.resultsline.data.PatientRecord;
import com.resultsline.identity.CallSession;
import com.resultsline.rules.IntervalRuleEngine;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answer or hand off, for a verified caller. Order of events:
 * identity check (done by {@link CallSession}) then Claude then parse then validate.
 * Anything that goes wrong becomes a handoff to staff, never a guessed answer.
 */
public class ResultsAssistant {

    private static final Logger LOG = LoggerFactory.getLogger(ResultsAssistant.class);

    private final ClaudeClient claude;
    private final IntervalRuleEngine rules;

    public ResultsAssistant(ClaudeClient claude, IntervalRuleEngine rules) {
        this.claude = claude;
        this.rules = rules;
    }

    public AssistantResult ask(CallSession session, String question) {
        PatientRecord patient = session.verifiedPatient().orElse(null);
        if (patient == null) {
            // Hard rule 5: no record information, and Claude is never called, before verification.
            return AssistantResult.forcedHandoff("caller is not verified", List.of("caller not verified"));
        }
        if (question == null || question.isBlank()) {
            return AssistantResult.forcedHandoff("no question was asked", List.of("empty question"));
        }

        Map<String, Object> facts = RecordFacts.of(patient, rules.followUpYears(patient.toPolypRecord()));

        String raw;
        try {
            raw = claude.complete(PromptBuilder.SYSTEM_PROMPT, PromptBuilder.userMessage(facts, question));
        } catch (RuntimeException e) {
            LOG.warn("Claude call failed: {}", e.getMessage());
            return AssistantResult.forcedHandoff("the assistant could not answer",
                    List.of("claude call failed: " + e.getMessage()));
        }

        AssistantReply reply;
        try {
            reply = ReplyParser.parse(raw);
        } catch (ReplyFormatException e) {
            LOG.warn("Reply rejected (structure): {}", e.getMessage());
            return AssistantResult.forcedHandoff("the assistant could not answer",
                    List.of("bad reply structure: " + e.getMessage()));
        }

        if (reply instanceof AssistantReply.Answer answer) {
            List<String> problems = ReplyValidator.validate(answer, facts);
            if (!problems.isEmpty()) {
                LOG.warn("Reply rejected (validation): {}", problems);
                return AssistantResult.forcedHandoff("the assistant could not answer", problems);
            }
        }
        return AssistantResult.accepted(reply);
    }
}
