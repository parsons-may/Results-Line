package com.resultsline.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Claude settings from application.yml. The API key is NOT here: it is read from the
 * ANTHROPIC_API_KEY environment variable (hard rule 8).
 */
@ConfigurationProperties(prefix = "results-line.claude")
public record ClaudeConfig(String model, long maxTokens) {
}
