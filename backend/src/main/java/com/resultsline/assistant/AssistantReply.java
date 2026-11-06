package com.resultsline.assistant;

import java.util.List;

/**
 * The fixed reply structure (the prompt contract). Claude replies in exactly one of two shapes:
 * an answer plus the record fields used, or a handoff to staff.
 */
public sealed interface AssistantReply {

    /** @param fieldsUsed record fields the answer relies on; must be non-empty and valid */
    record Answer(String text, List<String> fieldsUsed) implements AssistantReply {
    }

    /** @param reason short reason, for staff and the evaluation log */
    record Handoff(String reason) implements AssistantReply {
    }
}
