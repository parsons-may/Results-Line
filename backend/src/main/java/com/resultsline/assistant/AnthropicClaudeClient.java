package com.resultsline.assistant;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real Claude call through the official Anthropic Java SDK. The SDK client is created on first use,
 * so the app and the tests start without an API key; the key is read from ANTHROPIC_API_KEY.
 * Used unless results-line.claude.demo-mode is true.
 */
@Component
@ConditionalOnProperty(name = "results-line.claude.demo-mode", havingValue = "false", matchIfMissing = true)
public class AnthropicClaudeClient implements ClaudeClient {

    private final ClaudeConfig config;
    private AnthropicClient client;

    public AnthropicClaudeClient(ClaudeConfig config) {
        this.config = config;
    }

    @Override
    public String complete(String systemPrompt, String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(config.maxTokens())
                .system(systemPrompt)
                .addUserMessage(userMessage)
                .build();

        Message response = client().messages().create(params);

        // Refusals, truncation and anything else abnormal must not reach the validator as a "reply".
        boolean finishedNormally = response.stopReason()
                .map(reason -> reason.equals(StopReason.END_TURN))
                .orElse(false);
        if (!finishedNormally) {
            throw new IllegalStateException("Claude did not finish normally: stop reason "
                    + response.stopReason().map(Object::toString).orElse("none"));
        }

        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        return text.toString();
    }

    private synchronized AnthropicClient client() {
        if (client == null) {
            client = AnthropicOkHttpClient.fromEnv();
        }
        return client;
    }
}
