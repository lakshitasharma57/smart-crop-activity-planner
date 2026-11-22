package com.cropplanner.service;

import com.cropplanner.exception.PlannerException;
import com.cropplanner.model.ActivityTemplate;
import com.cropplanner.model.ActivityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Predefined irrigation, fertilizer and harvest schedules, keyed by crop name. */
public class ScheduleCatalog {

    private final Map<String, List<ActivityTemplate>> schedules = new HashMap<>();

    public ScheduleCatalog() {
        schedules.put("wheat", List.of(
                step(ActivityType.FERTILIZER, 0), step(ActivityType.IRRIGATION, 21),
                step(ActivityType.FERTILIZER, 25), step(ActivityType.IRRIGATION, 45),
                step(ActivityType.IRRIGATION, 70), step(ActivityType.IRRIGATION, 95),
                step(ActivityType.HARVESTING, 0)));
        schedules.put("rice", List.of(
                step(ActivityType.IRRIGATION, 7), step(ActivityType.FERTILIZER, 15),
                step(ActivityType.IRRIGATION, 25), step(ActivityType.FERTILIZER, 40),
                step(ActivityType.IRRIGATION, 55), step(ActivityType.IRRIGATION, 80),
                step(ActivityType.HARVESTING, 0)));
        schedules.put("maize", List.of(
                step(ActivityType.FERTILIZER, 0), step(ActivityType.IRRIGATION, 15),
                step(ActivityType.FERTILIZER, 30), step(ActivityType.IRRIGATION, 45),
                step(ActivityType.IRRIGATION, 65), step(ActivityType.HARVESTING, 0)));
        schedules.put("cotton", List.of(
                step(ActivityType.IRRIGATION, 20), step(ActivityType.FERTILIZER, 35),
                step(ActivityType.IRRIGATION, 50), step(ActivityType.IRRIGATION, 80),
                step(ActivityType.FERTILIZER, 90), step(ActivityType.HARVESTING, 0)));
    }

    private static ActivityTemplate step(ActivityType type, int daysAfterSowing) {
        return new ActivityTemplate(type, daysAfterSowing);
    }

    public List<ActivityTemplate> getSchedule(String cropType) throws PlannerException {
        List<ActivityTemplate> schedule = schedules.get(cropType.trim().toLowerCase());
        if (schedule == null) {
            throw new PlannerException("No predefined schedule for '" + cropType
                    + "'. Supported crops: " + String.join(", ", getSupportedCrops()));
        }
        return schedule;
    }

    /** A TreeSet keeps the crop names unique and in alphabetical order. */
    public Set<String> getSupportedCrops() {
        return new TreeSet<>(schedules.keySet());
    }
}
