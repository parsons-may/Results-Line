package com.resultsline.assistant;

import com.resultsline.data.PatientRecord;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The only facts about a patient that Claude sees and that an answer may cite.
 * Name, date of birth and phone are deliberately left out: they are used for the identity
 * check and are not needed to answer questions.
 */
public final class RecordFacts {

    private RecordFacts() {
    }

    /** Field name to value, in a stable order. followUpYears comes from the rule engine. */
    public static Map<String, Object> of(PatientRecord patient, int followUpYears) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("procedureType", patient.procedureType());
        facts.put("procedureDate", patient.procedureDate().toString());
        facts.put("polypCount", patient.polypCount());
        facts.put("largestPolypMm", patient.largestPolypMm());
        facts.put("pathologyResult", patient.pathologyResult());
        facts.put("followUpYears", followUpYears);
        return facts;
    }
}
