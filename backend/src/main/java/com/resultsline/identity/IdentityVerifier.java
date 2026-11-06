package com.resultsline.identity;

import com.resultsline.data.PatientRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Plain-code identity check: full name and date of birth must both match the same record.
 * No record information is released until this passes (hard rule 5). No AI involved.
 */
public class IdentityVerifier {

    private final List<PatientRecord> patients;

    public IdentityVerifier(List<PatientRecord> patients) {
        this.patients = List.copyOf(patients);
    }

    public Optional<PatientRecord> verify(String fullName, LocalDate dateOfBirth) {
        if (fullName == null || fullName.isBlank() || dateOfBirth == null) {
            return Optional.empty();
        }
        String wanted = normalize(fullName);
        return patients.stream()
                .filter(p -> normalize(p.fullName()).equals(wanted) && p.dateOfBirth().equals(dateOfBirth))
                .findFirst();
    }

    private static String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
