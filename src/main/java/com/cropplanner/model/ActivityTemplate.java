package com.cropplanner.model;

import java.time.LocalDate;

/** One step of a predefined crop schedule. */
public class ActivityTemplate {

    private final ActivityType type;
    private final int daysAfterSowing;

    public ActivityTemplate(ActivityType type, int daysAfterSowing) {
        this.type = type;
        this.daysAfterSowing = daysAfterSowing;
    }

    /** Harvesting always falls on the expected harvest date; other steps count from sowing. */
    public LocalDate dueDateFor(CropRecord crop) {
        if (type == ActivityType.HARVESTING) {
            return crop.getExpectedHarvestDate();
        }
        return crop.getSowingDate().plusDays(daysAfterSowing);
    }

    public ActivityType getType() { return type; }
    public int getDaysAfterSowing() { return daysAfterSowing; }
}
