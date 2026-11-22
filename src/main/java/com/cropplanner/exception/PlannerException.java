package com.cropplanner.exception;

/** Checked exception for business-rule failures (duplicates, completed tasks, unknown crops). */
public class PlannerException extends Exception {

    private static final long serialVersionUID = 1L;

    public PlannerException(String message) {
        super(message);
    }
}
