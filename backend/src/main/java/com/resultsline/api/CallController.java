package com.resultsline.api;

import com.resultsline.assistant.AssistantReply;
import com.resultsline.assistant.AssistantResult;
import com.resultsline.assistant.ClaudeConfig;
import com.resultsline.assistant.ResultsAssistant;
import com.resultsline.identity.CallSession;
import com.resultsline.identity.IdentityVerifier;
import com.resultsline.identity.VerificationOutcome;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP API for the simulated call screen. Sessions live in memory only and use synthetic data.
 * Replies never expose why a check or an answer failed, only what the caller should hear.
 */
@RestController
@RequestMapping("/api")
public class CallController {

    static final String GREETING = "Thank you for calling Results Line. This is a simulation using made-up "
            + "patient data. To begin, please tell me your full name and date of birth.";
    static final String VERIFIED_MESSAGE =
            "Thank you, you're verified. What would you like to know about your results?";
    static final String FAILED_MESSAGE = "I couldn't verify those details. Please check them and try again.";
    static final String LOCKED_MESSAGE = "I can't verify you on this call, so I'll pass you to a staff member.";
    static final String HANDOFF_MESSAGE = "I'm not able to help with that, so I'll pass you to a staff member.";
    static final int MAX_QUESTION_CHARS = 500;

    private final IdentityVerifier verifier;
    private final ResultsAssistant assistant;
    private final ClaudeConfig claudeConfig;
    private final Map<String, CallSession> sessions = new ConcurrentHashMap<>();

    public CallController(IdentityVerifier verifier, ResultsAssistant assistant, ClaudeConfig claudeConfig) {
        this.verifier = verifier;
        this.assistant = assistant;
        this.claudeConfig = claudeConfig;
    }

    public record StartResponse(String sessionId, String greeting) {
    }

    public record VerifyRequest(String fullName, LocalDate dateOfBirth) {
    }

    public record VerifyResponse(VerificationOutcome outcome, String message) {
    }

    public record AskRequest(String question) {
    }

    /** type is "answer" or "handoff". fieldsUsed is empty for handoffs. reason is only set for handoffs. */
    public record AskResponse(String type, String text, List<String> fieldsUsed, String reason) {
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of("demoMode", claudeConfig.demoMode());
    }

    @PostMapping("/call/start")
    public StartResponse start() {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new CallSession(verifier));
        return new StartResponse(id, GREETING);
    }

    @PostMapping("/call/{id}/verify")
    public VerifyResponse verify(@PathVariable String id, @RequestBody VerifyRequest request) {
        CallSession session = session(id);
        VerificationOutcome outcome;
        synchronized (session) {
            outcome = session.verify(request.fullName(), request.dateOfBirth());
        }
        String message = switch (outcome) {
            case VERIFIED -> VERIFIED_MESSAGE;
            case FAILED -> FAILED_MESSAGE;
            case LOCKED -> LOCKED_MESSAGE;
        };
        return new VerifyResponse(outcome, message);
    }

    @PostMapping("/call/{id}/ask")
    public AskResponse ask(@PathVariable String id, @RequestBody AskRequest request) {
        CallSession session = session(id);
        String question = request.question();
        if (question == null || question.isBlank() || question.length() > MAX_QUESTION_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "question must be 1 to " + MAX_QUESTION_CHARS + " characters");
        }
        AssistantResult result;
        synchronized (session) {
            result = assistant.ask(session, question);
        }
        if (result.reply() instanceof AssistantReply.Answer answer) {
            return new AskResponse("answer", answer.text(), answer.fieldsUsed(), null);
        }
        AssistantReply.Handoff handoff = (AssistantReply.Handoff) result.reply();
        // Forced handoffs (problems not empty) hide their reason: it can describe internals.
        String reason = result.problems().isEmpty() ? handoff.reason() : null;
        return new AskResponse("handoff", HANDOFF_MESSAGE, List.of(), reason);
    }

    private CallSession session(String id) {
        CallSession session = sessions.get(id);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown call");
        }
        return session;
    }
}
