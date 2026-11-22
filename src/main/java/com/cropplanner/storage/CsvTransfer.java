package com.cropplanner.storage;

import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Task;

import java.io.IOException;
import java.util.Comparator;

/** Moves a whole {@link PlannerData} to and from a folder of three CSV files. */
public class CsvTransfer {

    private static final String FARM_HEADER = "id,name,location,ownerName,ownerPhone";
    private static final String CROP_HEADER =
            "id,farmId,fieldName,cropType,areaAcres,sowingDate,expectedHarvestDate";
    private static final String TASK_HEADER =
            "id,cropId,type,scheduledDate,workerName,workerPhone,estimatedCost,status,completedDate,actualCost";

    public PlannerData importFrom(String directory) throws IOException {
        PlannerData data = new PlannerData();
        for (Farm farm : new CsvStore<>(directory + "/farms.csv", FARM_HEADER, Farm::fromCsv).load()) {
            data.getFarms().put(farm.getId(), farm);
        }
        for (CropRecord crop : new CsvStore<>(directory + "/crops.csv", CROP_HEADER, CropRecord::fromCsv).load()) {
            data.getCrops().put(crop.getId(), crop);
        }
        data.getTasks().addAll(new CsvStore<>(directory + "/tasks.csv", TASK_HEADER, Task::fromCsv).load());
        return data;
    }

    /** Rows are written in id order so the files stay tidy. */
    public void exportTo(PlannerData data, String directory) throws IOException {
        new CsvStore<>(directory + "/farms.csv", FARM_HEADER, Farm::fromCsv).save(
                data.getFarms().values().stream().sorted(Comparator.comparingInt(Farm::getId)).toList());
        new CsvStore<>(directory + "/crops.csv", CROP_HEADER, CropRecord::fromCsv).save(
                data.getCrops().values().stream().sorted(Comparator.comparingInt(CropRecord::getId)).toList());
        new CsvStore<>(directory + "/tasks.csv", TASK_HEADER, Task::fromCsv).save(
                data.getTasks().stream().sorted(Comparator.comparingInt(Task::getId)).toList());
    }
}
