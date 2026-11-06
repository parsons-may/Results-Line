package com.resultsline.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Strictly parses Claude's text into an {@link AssistantReply}. Anything that is not exactly
 * one of the two allowed JSON shapes throws {@link ReplyFormatException}; the model is not trusted
 * to follow the structure (hard rule 4).
 *
 * <pre>
 * {"type":"answer","answer":"...","fields_used":["polypCount"]}
 * {"type":"handoff","reason":"..."}
 * </pre>
 */
public final class ReplyParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ANSWER_KEYS = Set.of("type", "answer", "fields_used");
    private static final Set<String> HANDOFF_KEYS = Set.of("type", "reason");

    private ReplyParser() {
    }

    public static AssistantReply parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ReplyFormatException("empty reply");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(raw.trim());
        } catch (JsonProcessingException e) {
            throw new ReplyFormatException("reply is not valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw new ReplyFormatException("reply is not a JSON object");
        }
        JsonNode type = root.get("type");
        if (type == null || !type.isTextual()) {
            throw new ReplyFormatException("missing or non-text 'type'");
        }
        return switch (type.asText()) {
            case "answer" -> parseAnswer(root);
            case "handoff" -> parseHandoff(root);
            default -> throw new ReplyFormatException("unknown type '" + type.asText() + "'");
        };
    }

    private static AssistantReply parseAnswer(JsonNode root) {
        requireExactKeys(root, ANSWER_KEYS);
        JsonNode text = root.get("answer");
        if (!text.isTextual() || text.asText().isBlank()) {
            throw new ReplyFormatException("'answer' must be non-empty text");
        }
        JsonNode fields = root.get("fields_used");
        if (!fields.isArray()) {
            throw new ReplyFormatException("'fields_used' must be an array");
        }
        List<String> used = new ArrayList<>();
        for (JsonNode f : fields) {
            if (!f.isTextual()) {
                throw new ReplyFormatException("'fields_used' must contain only text");
            }
            used.add(f.asText());
        }
        return new AssistantReply.Answer(text.asText(), List.copyOf(used));
    }

    private static AssistantReply parseHandoff(JsonNode root) {
        requireExactKeys(root, HANDOFF_KEYS);
        JsonNode reason = root.get("reason");
        if (!reason.isTextual() || reason.asText().isBlank()) {
            throw new ReplyFormatException("'reason' must be non-empty text");
        }
        return new AssistantReply.Handoff(reason.asText());
    }

    private static void requireExactKeys(JsonNode root, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        root.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new ReplyFormatException("unexpected keys " + actual + ", expected " + expected);
        }
    }
}
