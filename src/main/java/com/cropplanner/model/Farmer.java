package com.cropplanner.model;

public class Farmer extends Person {

    public Farmer(String name, String phone) {
        super(name, phone);
    }

    @Override
    public String getRole() {
        return "Farmer";
    }
}
