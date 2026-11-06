package com.resultsline.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.resultsline.data.SyntheticDataLoader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IdentityTest {

    private final IdentityVerifier verifier = new IdentityVerifier(SyntheticDataLoader.loadPatients());
    private static final LocalDate TESTERSON_DOB = LocalDate.of(1968, 3, 14);

    @Test
    void matchingNameAndDobVerifies() {
        assertThat(verifier.verify("Alex Testerson", TESTERSON_DOB)).isPresent();
    }

    @Test
    void nameMatchIgnoresCaseAndExtraSpaces() {
        assertThat(verifier.verify("  alex   TESTERSON ", TESTERSON_DOB)).isPresent();
    }

    @Test
    void wrongDobFails() {
        assertThat(verifier.verify("Alex Testerson", LocalDate.of(1968, 3, 15))).isEmpty();
    }

    @Test
    void wrongNameFails() {
        assertThat(verifier.verify("Alex Testersen", TESTERSON_DOB)).isEmpty();
    }

    @Test
    void nameAndDobFromDifferentPatientsFails() {
        // Blair Samplewood's name with Alex Testerson's date of birth
        assertThat(verifier.verify("Blair Samplewood", TESTERSON_DOB)).isEmpty();
    }

    @Test
    void blankOrMissingInputFails() {
        assertThat(verifier.verify("", TESTERSON_DOB)).isEmpty();
        assertThat(verifier.verify(null, TESTERSON_DOB)).isEmpty();
        assertThat(verifier.verify("Alex Testerson", null)).isEmpty();
    }

    @Test
    void sessionVerifiesAndExposesOnlyTheMatchedPatient() {
        CallSession session = new CallSession(verifier);
        assertThat(session.verifiedPatient()).isEmpty();
        assertThat(session.verify("Alex Testerson", TESTERSON_DOB)).isEqualTo(VerificationOutcome.VERIFIED);
        assertThat(session.verifiedPatient()).get().extracting("patientId").isEqualTo("P001");
    }

    @Test
    void sessionLocksAfterThreeFailedAttempts() {
        CallSession session = new CallSession(verifier);
        assertThat(session.verify("Nobody Here", TESTERSON_DOB)).isEqualTo(VerificationOutcome.FAILED);
        assertThat(session.verify("Nobody Here", TESTERSON_DOB)).isEqualTo(VerificationOutcome.FAILED);
        assertThat(session.verify("Nobody Here", TESTERSON_DOB)).isEqualTo(VerificationOutcome.LOCKED);
        assertThat(session.isLocked()).isTrue();
    }

    @Test
    void lockedSessionRejectsEvenCorrectDetails() {
        CallSession session = new CallSession(verifier);
        for (int i = 0; i < CallSession.MAX_FAILED_ATTEMPTS; i++) {
            session.verify("Nobody Here", TESTERSON_DOB);
        }
        assertThat(session.verify("Alex Testerson", TESTERSON_DOB)).isEqualTo(VerificationOutcome.LOCKED);
        assertThat(session.verifiedPatient()).isEmpty();
    }
}
