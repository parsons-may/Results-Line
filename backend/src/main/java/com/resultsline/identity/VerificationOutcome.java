package com.resultsline.identity;

/** Result of one identity attempt. FAILED and LOCKED give the caller no hint about why. */
public enum VerificationOutcome {
    VERIFIED,
    FAILED,
    /** Too many failed attempts: the call goes to staff. */
    LOCKED
}
