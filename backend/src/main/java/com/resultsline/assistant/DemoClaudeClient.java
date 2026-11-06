package com.resultsline.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEMO ONLY. A simple keyword script that stands in for Claude so the screen can be built and tried
 * without an API key. It is not an AI and not a measure of how Claude behaves. Its replies still go
 * through the same parser and validator as real Claude replies. The UI shows a banner while it is on.
 * Enabled only when results-line.claude.demo-mode is true.
 */
@Component
@ConditionalOnProperty(name = "results-line.claude.demo-mode", havingValue = "true")
public class DemoClaudeClient implements ClaudeClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Words that mean the question needs staff, checked before any answer keyword. */
    private static final List<String> HANDOFF_WORDS = List.of(
            "worry", "fine", "normal", "good", "bad", "cancer", "serious", "dangerous", "should i", "okay",
            "safe", "clear", "skip", "pain", "hurt", "blood", "wife", "husband", "brother", "sister", "mother",
            "father", "book", "cost", "price", "weather", "ignore", "diet", "alcohol", "medic", "aspirin");

    @Override
    public String complete(String systemPrompt, String userMessage) {
        JsonNode facts = readFacts(userMessage);
        String question = readQuestion(userMessage).toLowerCase();

        for (String word : HANDOFF_WORDS) {
            if (question.contains(word)) {
                return handoff("demo script: needs staff");
            }
        }
        if (containsAny(question, "next", "follow", "how long", "come back", "interval")) {
            return answer("Your follow-up interval is " + facts.get("followUpYears").asInt() + " years.",
                    "followUpYears");
        }
        if (containsAny(question, "how many", "polyp") && !containsAny(question, "lab", "kind", "type")) {
            int n = facts.get("polypCount").asInt();
            String text = n == 0 ? "Your record shows no polyps were found."
                    : "Your record shows " + n + (n == 1 ? " polyp." : " polyps.");
            return answer(text, "polypCount");
        }
        if (containsAny(question, "big", "size", "mm", "largest", "biggest")) {
            int mm = facts.get("largestPolypMm").asInt();
            if (mm == 0) {
                return answer("No polyps were found, so there is no size to report.", "polypCount");
            }
            return answer("The largest polyp measured " + mm + " mm.", "largestPolypMm");
        }
        if (containsAny(question, "lab", "pathology", "kind", "type")) {
            return answer("Your lab result says: " + facts.get("pathologyResult").asText() + ".", "pathologyResult");
        }
        if (containsAny(question, "when", "date")) {
            return answer("Your procedure was on " + facts.get("procedureDate").asText() + ".", "procedureDate");
        }
        return handoff("demo script: not understood");
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static String answer(String text, String field) {
        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("type", "answer");
        reply.put("answer", text);
        reply.put("fields_used", List.of(field));
        return toJson(reply);
    }

    private static String handoff(String reason) {
        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("type", "handoff");
        reply.put("reason", reason);
        return toJson(reply);
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode readFacts(String userMessage) {
        int start = userMessage.indexOf('{');
        int end = userMessage.indexOf("\n\nCALLER QUESTION");
        try {
            return MAPPER.readTree(userMessage.substring(start, end));
        } catch (JsonProcessingException | RuntimeException e) {
            throw new IllegalStateException("Demo client could not read the record in the prompt", e);
        }
    }

    private static String readQuestion(String userMessage) {
        String open = "<question>\n";
        int start = userMessage.indexOf(open) + open.length();
        int end = userMessage.lastIndexOf("\n</question>");
        return userMessage.substring(start, end);
    }
}
