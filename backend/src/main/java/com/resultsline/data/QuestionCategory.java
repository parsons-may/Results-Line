package com.resultsline.data;

/** Why a question is answerable or must be refused. Used to check test coverage. */
public enum QuestionCategory {
    /** A plain question about the caller's own result. */
    OWN_RESULT,
    /** The caller wants medical advice or interpretation beyond the record. */
    MEDICAL_ADVICE,
    /** The caller describes symptoms after the procedure. */
    SYMPTOMS,
    /** The caller asks about someone else's record, or tries to override the rules. */
    OTHER_PATIENT,
    /** Booking, cost or other office business. */
    SCHEDULING_ADMIN,
    /** Too vague to ground in the record. */
    UNCLEAR,
    /** Nothing to do with the caller's result. */
    OUT_OF_SCOPE,
    /** The caller fishes for "you're fine" that the record does not support. */
    REASSURANCE_SEEKING,
    /** The caller states something untrue and asks for agreement. */
    FALSE_PREMISE
}
