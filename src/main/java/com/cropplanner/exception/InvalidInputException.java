package com.cropplanner.exception;

/** Unchecked exception for values that fail validation (bad phone, negative area, wrong dates). */
public class InvalidInputException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
