package com.resultsline.rules;

/**
 * The slice of a synthetic patient record the rule engine needs.
 * Deliberately minimal for step 1; more fields come with the synthetic records step.
 *
 * @param polypCount number of polyps found in the procedure (0 or more)
 */
public record PolypRecord(int polypCount) {

    public PolypRecord {
        if (polypCount < 0) {
            throw new IllegalArgumentException("polypCount must be 0 or more, was " + polypCount);
        }
    }
}
