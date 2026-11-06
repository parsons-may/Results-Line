package com.resultsline.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Runs the API with the demo client, so no API key is needed. */
@SpringBootTest(properties = "results-line.claude.demo-mode=true")
@AutoConfigureMockMvc
class CallControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    private String startCall() throws Exception {
        String body = mvc.perform(post("/api/call/start")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("sessionId").asText();
    }

    private void verify(String id, String name, String dob, String expectedOutcome) throws Exception {
        mvc.perform(post("/api/call/" + id + "/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + name + "\",\"dateOfBirth\":\"" + dob + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value(expectedOutcome));
    }

    private org.springframework.test.web.servlet.ResultActions ask(String id, String question) throws Exception {
        return mvc.perform(post("/api/call/" + id + "/ask").contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"" + question + "\"}"));
    }

    @Test
    void infoReportsDemoMode() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk()).andExpect(jsonPath("$.demoMode").value(true));
    }

    @Test
    void startReturnsSessionAndGreeting() throws Exception {
        mvc.perform(post("/api/call/start")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.greeting").value(CallController.GREETING));
    }

    @Test
    void askingBeforeVerificationGetsHandoffAndNoRecordInfo() throws Exception {
        String id = startCall();
        ask(id, "How many polyps did I have?").andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("handoff"))
                .andExpect(jsonPath("$.text").value(CallController.HANDOFF_MESSAGE))
                .andExpect(jsonPath("$.fieldsUsed", hasSize(0)))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void wrongDetailsFailThenLockAfterThree() throws Exception {
        String id = startCall();
        verify(id, "Nobody Here", "1990-01-01", "FAILED");
        verify(id, "Nobody Here", "1990-01-01", "FAILED");
        verify(id, "Nobody Here", "1990-01-01", "LOCKED");
        verify(id, "Finley Placeholder", "1963-09-09", "LOCKED");
    }

    @Test
    void failedVerificationMessageDoesNotSayWhatWasWrong() throws Exception {
        String id = startCall();
        mvc.perform(post("/api/call/" + id + "/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Finley Placeholder\",\"dateOfBirth\":\"1999-01-01\"}"))
                .andExpect(jsonPath("$.message").value(CallController.FAILED_MESSAGE))
                .andExpect(content().string(not(containsString("date of birth"))));
    }

    @Test
    void verifiedCallerGetsGroundedAnswerWithFieldsUsed() throws Exception {
        String id = startCall();
        verify(id, "Finley Placeholder", "1963-09-09", "VERIFIED"); // P006: 5 polyps
        ask(id, "How many polyps did I have?").andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("answer"))
                .andExpect(jsonPath("$.text").value("Your record shows 5 polyps."))
                .andExpect(jsonPath("$.fieldsUsed", hasItem("polypCount")));
    }

    @Test
    void verifiedCallerAskingForReassuranceGetsHandoff() throws Exception {
        String id = startCall();
        verify(id, "Finley Placeholder", "1963-09-09", "VERIFIED");
        ask(id, "So I'm fine, right?").andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("handoff"))
                .andExpect(jsonPath("$.text").value(CallController.HANDOFF_MESSAGE));
    }

    @Test
    void unknownSessionIs404() throws Exception {
        ask("does-not-exist", "hello").andExpect(status().isNotFound());
    }

    @Test
    void blankOrTooLongQuestionIs400() throws Exception {
        String id = startCall();
        verify(id, "Finley Placeholder", "1963-09-09", "VERIFIED");
        ask(id, "   ").andExpect(status().isBadRequest());
        ask(id, "x".repeat(CallController.MAX_QUESTION_CHARS + 1)).andExpect(status().isBadRequest());
    }

    @Test
    void badDateFormatIs400() throws Exception {
        String id = startCall();
        mvc.perform(post("/api/call/" + id + "/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Finley Placeholder\",\"dateOfBirth\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest());
    }
}
