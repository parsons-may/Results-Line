package com.resultsline.eval;

import com.resultsline.assistant.ClaudeClient;
import com.resultsline.assistant.ClaudeConfig;
import com.resultsline.assistant.ResultsAssistant;
import com.resultsline.data.SyntheticDataLoader;
import com.resultsline.identity.IdentityVerifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the evaluation and writes the results log. Only active with --results-line.eval.run=true,
 * normally through ./run-eval.sh. Never part of the normal app or the tests.
 */
@Component
@ConditionalOnProperty(name = "results-line.eval.run", havingValue = "true")
public class EvalCommand implements ApplicationRunner {

    private final ResultsAssistant assistant;
    private final IdentityVerifier verifier;
    private final ClaudeClient claude;
    private final ClaudeConfig config;
    private final String outputDir;

    public EvalCommand(ResultsAssistant assistant, IdentityVerifier verifier, ClaudeClient claude,
                       ClaudeConfig config, @Value("${results-line.eval.output-dir:../docs/results}") String outputDir) {
        this.assistant = assistant;
        this.verifier = verifier;
        this.claude = claude;
        this.config = config;
        this.outputDir = outputDir;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        String label = config.demoMode() ? "DEMO CLIENT, NOT CLAUDE" : "Claude (" + config.model() + ")";
        System.out.println("Running evaluation with: " + label);

        List<EvalRecord> records = new EvalRunner(assistant, verifier)
                .run(SyntheticDataLoader.loadPatients(), SyntheticDataLoader.loadQuestions());

        if (records.stream().allMatch(EvalRecord::claudeCallFailed)) {
            String first = records.get(0).result().problems().get(0);
            throw new IllegalStateException("Every Claude call failed, so there are no results to report. "
                    + "First error: " + first + ". Check ANTHROPIC_API_KEY and billing.");
        }

        LocalDateTime now = LocalDateTime.now();
        String markdown = EvalReportWriter.markdown(label, now.toLocalDate().toString(), records, claude.tokenUsage());

        String name = "eval-" + (config.demoMode() ? "DEMO-" : "")
                + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md";
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(name);
        Files.writeString(file, markdown);

        EvalSummary s = EvalSummary.of(records);
        System.out.println();
        System.out.println("FALSE REASSURANCE: " + s.falseReassurance() + " of " + s.total() + " questions ("
                + s.falseReassuranceOnTraps() + " of " + s.traps() + " traps)");
        System.out.println("Correct answers:   " + s.correctAnswers() + " of " + s.shouldAnswer());
        System.out.println("Over-refusals:     " + s.overRefusals() + " of " + s.shouldAnswer());
        System.out.println("Correct refusals:  " + s.correctRefusals() + " of " + s.shouldRefuse());
        System.out.println("Forced handoffs:   " + s.forcedHandoffs() + " of " + s.total());
        System.out.println("Tokens used:       " + claude.tokenUsage());
        System.out.println("Report written to: " + file.toAbsolutePath().normalize());
    }
}
