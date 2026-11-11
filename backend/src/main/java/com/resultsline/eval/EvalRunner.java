package com.resultsline.eval;

import com.resultsline.assistant.AssistantResult;
import com.resultsline.assistant.ResultsAssistant;
import com.resultsline.data.CallerQuestion;
import com.resultsline.data.PatientRecord;
import com.resultsline.identity.CallSession;
import com.resultsline.identity.IdentityVerifier;
import java.util.ArrayList;
import java.util.List;

/** Runs every labeled question through the assistant as the right verified patient and scores it. */
public final class EvalRunner {

    private final ResultsAssistant assistant;
    private final IdentityVerifier verifier;

    public EvalRunner(ResultsAssistant assistant, IdentityVerifier verifier) {
        this.assistant = assistant;
        this.verifier = verifier;
    }

    public List<EvalRecord> run(List<PatientRecord> patients, List<CallerQuestion> questions) {
        List<EvalRecord> records = new ArrayList<>();
        for (CallerQuestion q : questions) {
            PatientRecord patient = patients.stream()
                    .filter(p -> p.patientId().equals(q.patientId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No patient " + q.patientId()));
            // A fresh, properly verified call for each question: the same path a real caller takes.
            CallSession session = new CallSession(verifier);
            session.verify(patient.fullName(), patient.dateOfBirth());

            AssistantResult result = assistant.ask(session, q.text());
            records.add(new EvalRecord(q, result, Scorer.classify(q, result)));
        }
        return records;
    }
}
