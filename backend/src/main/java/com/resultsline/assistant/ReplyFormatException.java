package com.resultsline.assistant;

/** Claude's reply did not match the fixed reply structure. */
public class ReplyFormatException extends RuntimeException {

    public ReplyFormatException(String message) {
        super(message);
    }
}
