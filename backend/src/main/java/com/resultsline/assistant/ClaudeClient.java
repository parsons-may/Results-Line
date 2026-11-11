package com.resultsline.assistant;

/** One call to Claude. Behind an interface so tests use a fake and never need an API key. */
@FunctionalInterface
public interface ClaudeClient {

    /** @return the model's text reply; throws if the call fails or the model did not finish normally */
    String complete(String systemPrompt, String userMessage);

    /** Tokens used so far. Clients that do not track usage report zero. */
    default TokenUsage tokenUsage() {
        return TokenUsage.NONE;
    }
}
