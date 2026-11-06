package com.resultsline.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Checks the synthetic records and labeled questions are well formed and consistent. */
class SyntheticDataTest {

    private final List<PatientRecord> patients = SyntheticDataLoader.loadPatients();
    private final List<CallerQuestion> questions = SyntheticDataLoader.loadQuestions();
    private final Map<String, PatientRecord> byId =
            patients.stream().collect(Collectors.toMap(PatientRecord::patientId, Function.identity()));

    @Test
    void patientsLoadWithUniqueIds() {
        assertThat(patients).hasSizeGreaterThanOrEqualTo(8);
        assertThat(patients.stream().map(PatientRecord::patientId).collect(Collectors.toSet()))
                .hasSameSizeAs(patients);
    }

    @Test
    void phonesAreInReserved555Range() {
        assertThat(patients).allSatisfy(p -> assertThat(p.phone()).matches("555-01\\d\\d"));
    }

    @Test
    void recordsHaveSensibleValues() {
        assertThat(patients).allSatisfy(p -> {
            assertThat(p.polypCount()).isGreaterThanOrEqualTo(0);
            assertThat(p.largestPolypMm()).isGreaterThanOrEqualTo(0);
            assertThat(p.dateOfBirth()).isBefore(p.procedureDate());
            assertThat(p.pathologyResult()).isNotBlank();
            // no polyps means no polyp size; polyps present means a size
            assertThat(p.polypCount() == 0).isEqualTo(p.largestPolypMm() == 0);
        });
    }

    @Test
    void recordsCoverTheRuleBoundary() {
        Set<Integer> counts = patients.stream().map(PatientRecord::polypCount).collect(Collectors.toSet());
        assertThat(counts).contains(0, 4, 5);
        assertThat(counts.stream().anyMatch(c -> c > 5)).isTrue();
    }

    @Test
    void questionsLoadWithUniqueIds() {
        assertThat(questions).hasSizeGreaterThanOrEqualTo(40);
        assertThat(questions.stream().map(CallerQuestion::questionId).collect(Collectors.toSet()))
                .hasSameSizeAs(questions);
    }

    @Test
    void everyQuestionPointsAtARealPatient() {
        assertThat(questions).allSatisfy(q -> assertThat(byId).containsKey(q.patientId()));
    }

    @Test
    void answerQuestionsCiteKnownFieldsAndRefusalsCiteNone() {
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.text()).isNotBlank();
            if (q.expected() == ExpectedBehavior.ANSWER) {
                assertThat(q.requiredFields()).isNotEmpty();
                assertThat(PatientRecord.CITABLE_FIELDS).containsAll(q.requiredFields());
            } else {
                assertThat(q.requiredFields()).isEmpty();
            }
        });
    }

    @Test
    void questionsIncludeAnswersRefusalsAndFalseReassuranceTraps() {
        assertThat(questions).anyMatch(q -> q.expected() == ExpectedBehavior.ANSWER);
        assertThat(questions).anyMatch(q -> q.expected() == ExpectedBehavior.REFUSE);
        assertThat(questions).filteredOn(CallerQuestion::falseReassuranceTrap).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void everyCategoryIsCovered() {
        Set<QuestionCategory> used = questions.stream().map(CallerQuestion::category).collect(Collectors.toSet());
        assertThat(used).containsExactlyInAnyOrder(QuestionCategory.values());
    }

    @Test
    void reassuranceTrapsAreNotAskedAboutLowRiskRecordsExceptNoPolypCase() {
        // Sanity check that traps are mostly about records with something to worry about.
        long trapsOnHigherRisk = questions.stream()
                .filter(CallerQuestion::falseReassuranceTrap)
                .filter(q -> byId.get(q.patientId()).polypCount() >= 4)
                .count();
        assertThat(trapsOnHigherRisk).isGreaterThanOrEqualTo(4);
    }
}
