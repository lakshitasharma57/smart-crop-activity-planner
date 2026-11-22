package com.cropplanner.model;

/** The three kinds of farm activity, each with a default cost rate per acre (Rs). */
public enum ActivityType {

    IRRIGATION("Irrigation", 700),
    FERTILIZER("Fertilizer application", 1800),
    HARVESTING("Harvesting", 2500);

    private final String label;
    private final double costPerAcre;

    ActivityType(String label, double costPerAcre) {
        this.label = label;
        this.costPerAcre = costPerAcre;
    }

    public double estimateCost(double areaAcres) {
        return costPerAcre * areaAcres;
    }

    @Override
    public String toString() {
        return label;
    }
}
