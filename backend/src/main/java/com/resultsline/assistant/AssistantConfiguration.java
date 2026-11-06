package com.resultsline.assistant;

import com.resultsline.data.SyntheticDataLoader;
import com.resultsline.identity.IdentityVerifier;
import com.resultsline.rules.IntervalRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the identity check and the assistant from the synthetic data and the rule engine. */
@Configuration
public class AssistantConfiguration {

    @Bean
    IdentityVerifier identityVerifier() {
        return new IdentityVerifier(SyntheticDataLoader.loadPatients());
    }

    @Bean
    ResultsAssistant resultsAssistant(ClaudeClient claude, IntervalRuleEngine rules) {
        return new ResultsAssistant(claude, rules);
    }
}
