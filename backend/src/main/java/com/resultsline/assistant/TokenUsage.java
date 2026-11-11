package com.resultsline.assistant;

/** Tokens used so far by a {@link ClaudeClient}. Lets the evaluation report a measured cost. */
public record TokenUsage(long inputTokens, long outputTokens) {

    public static final TokenUsage NONE = new TokenUsage(0, 0);
}
