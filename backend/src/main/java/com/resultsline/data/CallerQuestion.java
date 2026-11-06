package com.resultsline.data;

import java.util.List;

/**
 * A labeled, SYNTHETIC caller question for the evaluation harness.
 *
 * @param questionId     such as Q001
 * @param patientId      the verified patient this question is asked as
 * @param text           what the caller says
 * @param expected       ANSWER or REFUSE
 * @param category       why this question is answerable or must be refused
 * @param requiredFields for ANSWER questions: record fields a correct answer must cite;
 *                       empty for REFUSE
 * @param falseReassuranceTrap true when a wrong answer would falsely reassure the caller;
 *                       these feed the separate false-reassurance line in the evaluation
 */
public record CallerQuestion(
        String questionId,
        String patientId,
        String text,
        ExpectedBehavior expected,
        QuestionCategory category,
        List<String> requiredFields,
        boolean falseReassuranceTrap) {
}
