package com.resultsline.data;

import com.resultsline.rules.PolypRecord;
import java.time.LocalDate;
import java.util.Set;

/**
 * A SYNTHETIC patient record. Every value is made up. No real patient data, ever.
 *
 * @param patientId       synthetic ID such as P001
 * @param fullName        made-up name
 * @param dateOfBirth     made-up date of birth (used later for the identity check)
 * @param phone           made-up number in the reserved 555-01xx range
 * @param procedureType   e.g. "colonoscopy"
 * @param procedureDate   date of the procedure
 * @param polypCount      number of polyps found
 * @param largestPolypMm  size of the largest polyp in millimetres (0 if none)
 * @param pathologyResult lab result text, or "pending"
 */
public record PatientRecord(
        String patientId,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String procedureType,
        LocalDate procedureDate,
        int polypCount,
        int largestPolypMm,
        String pathologyResult) {

    /**
     * Field names an answer is allowed to cite. followUpYears is not stored in the
     * record; it is computed by the rule engine from the record.
     */
    public static final Set<String> CITABLE_FIELDS = Set.of(
            "procedureType", "procedureDate", "polypCount",
            "largestPolypMm", "pathologyResult", "followUpYears");

    /** The part of this record the rule engine needs. */
    public PolypRecord toPolypRecord() {
        return new PolypRecord(polypCount);
    }
}
