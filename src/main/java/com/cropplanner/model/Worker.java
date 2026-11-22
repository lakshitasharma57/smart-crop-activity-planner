package com.cropplanner.model;

public class Worker extends Person {

    public Worker(String name, String phone) {
        super(name, phone);
    }

    @Override
    public String getRole() {
        return "Worker";
    }
}
