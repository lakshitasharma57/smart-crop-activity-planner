package com.cropplanner.storage;

import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All in-memory data: farms and crops are looked up by id, tasks keep their order. */
public class PlannerData {

    private final Map<Integer, Farm> farms = new HashMap<>();
    private final Map<Integer, CropRecord> crops = new HashMap<>();
    private final List<Task> tasks = new ArrayList<>();

    public Map<Integer, Farm> getFarms() { return farms; }
    public Map<Integer, CropRecord> getCrops() { return crops; }
    public List<Task> getTasks() { return tasks; }

    public int nextFarmId() {
        return maxId(farms.keySet()) + 1;
    }

    public int nextCropId() {
        return maxId(crops.keySet()) + 1;
    }

    public int nextTaskId() {
        int max = 0;
        for (Task t : tasks) {
            max = Math.max(max, t.getId());
        }
        return max + 1;
    }

    private static int maxId(Collection<Integer> ids) {
        int max = 0;
        for (int id : ids) {
            max = Math.max(max, id);
        }
        return max;
    }
}
