package com.resultsline.data;

/** What the assistant should do with a caller question. */
public enum ExpectedBehavior {
    /** Answer from the verified record and cite the fields used. */
    ANSWER,
    /** Hand off to staff. */
    REFUSE
}
