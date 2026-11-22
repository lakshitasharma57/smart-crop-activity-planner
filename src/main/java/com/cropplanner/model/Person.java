package com.cropplanner.model;

import com.cropplanner.exception.InvalidInputException;

public abstract class Person {

    private final String name;
    private final String phone;

    protected Person(String name, String phone) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Name cannot be empty");
        }
        if (phone == null || !phone.trim().matches("\\d{10}")) {
            throw new InvalidInputException("Phone number must be exactly 10 digits");
        }
        this.name = name.trim();
        this.phone = phone.trim();
    }

    public abstract String getRole();

    public String getName() { return name; }
    public String getPhone() { return phone; }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, getRole(), phone);
    }
}
