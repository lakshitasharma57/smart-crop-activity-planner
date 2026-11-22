package com.cropplanner.model;

import com.cropplanner.exception.InvalidInputException;

public class Farm implements CsvSerializable {

    private final int id;
    private final String name;
    private final String location;
    private final Farmer owner;

    public Farm(int id, String name, String location, Farmer owner) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Farm name cannot be empty");
        }
        if (location == null || location.isBlank()) {
            throw new InvalidInputException("Location cannot be empty");
        }
        if (owner == null) {
            throw new InvalidInputException("A farm needs an owner");
        }
        this.id = id;
        this.name = name.trim();
        this.location = location.trim();
        this.owner = owner;
    }

    public static Farm fromCsv(String[] p) {
        if (p.length < 5) {
            throw new InvalidInputException("expected 5 fields but found " + p.length);
        }
        return new Farm(Integer.parseInt(p[0].trim()), p[1], p[2], new Farmer(p[3], p[4]));
    }

    @Override
    public String toCsv() {
        return String.join(",", String.valueOf(id), CsvSerializable.clean(name),
                CsvSerializable.clean(location), CsvSerializable.clean(owner.getName()), owner.getPhone());
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public Farmer getOwner() { return owner; }

    @Override
    public String toString() {
        return String.format("Farm #%d  %s, %s  | Owner: %s", id, name, location, owner);
    }
}
