package com.resultsline.identity;

import com.resultsline.data.PatientRecord;
import java.time.LocalDate;
import java.util.Optional;

/** State of one simulated call: whether the caller is verified and how many attempts failed. */
public class CallSession {

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final IdentityVerifier verifier;
    private int failedAttempts;
    private PatientRecord verifiedPatient;

    public CallSession(IdentityVerifier verifier) {
        this.verifier = verifier;
    }

    public VerificationOutcome verify(String fullName, LocalDate dateOfBirth) {
        if (verifiedPatient != null) {
            return VerificationOutcome.VERIFIED;
        }
        if (isLocked()) {
            return VerificationOutcome.LOCKED;
        }
        Optional<PatientRecord> match = verifier.verify(fullName, dateOfBirth);
        if (match.isPresent()) {
            verifiedPatient = match.get();
            return VerificationOutcome.VERIFIED;
        }
        failedAttempts++;
        return isLocked() ? VerificationOutcome.LOCKED : VerificationOutcome.FAILED;
    }

    public Optional<PatientRecord> verifiedPatient() {
        return Optional.ofNullable(verifiedPatient);
    }

    public boolean isLocked() {
        return verifiedPatient == null && failedAttempts >= MAX_FAILED_ATTEMPTS;
    }
}
