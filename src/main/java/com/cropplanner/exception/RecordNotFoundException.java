package com.cropplanner.exception;

public class RecordNotFoundException extends PlannerException {

    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String kind, int id) {
        super(kind + " with id " + id + " was not found");
    }
}
