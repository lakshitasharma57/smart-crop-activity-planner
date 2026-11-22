package com.cropplanner.service;

import com.cropplanner.exception.PlannerException;
import com.cropplanner.exception.RecordNotFoundException;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Farmer;
import com.cropplanner.storage.PlannerData;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Registers farms and their crop records. */
public class FarmService {

    private final PlannerData data;

    public FarmService(PlannerData data) {
        this.data = data;
    }

    public Farm registerFarm(String name, String location, Farmer owner) throws PlannerException {
        Farm farm = new Farm(data.nextFarmId(), name, location, owner);
        boolean duplicate = data.getFarms().values().stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(farm.getName()));
        if (duplicate) {
            throw new PlannerException("A farm named '" + farm.getName() + "' is already registered");
        }
        data.getFarms().put(farm.getId(), farm);
        return farm;
    }

    public CropRecord addCrop(int farmId, String fieldName, String cropType, double areaAcres,
                              LocalDate sowingDate, LocalDate expectedHarvestDate) throws PlannerException {
        getFarm(farmId);                                    // the farm must exist
        CropRecord crop = new CropRecord(data.nextCropId(), farmId, fieldName, cropType,
                areaAcres, sowingDate, expectedHarvestDate);
        data.getCrops().put(crop.getId(), crop);
        return crop;
    }

    public Farm getFarm(int id) throws RecordNotFoundException {
        Farm farm = data.getFarms().get(id);
        if (farm == null) {
            throw new RecordNotFoundException("Farm", id);
        }
        return farm;
    }

    public CropRecord getCrop(int id) throws RecordNotFoundException {
        CropRecord crop = data.getCrops().get(id);
        if (crop == null) {
            throw new RecordNotFoundException("Crop record", id);
        }
        return crop;
    }

    public List<Farm> getFarms() {
        return data.getFarms().values().stream()
                .sorted(Comparator.comparingInt(Farm::getId)).toList();
    }

    public List<CropRecord> getCrops() {
        return data.getCrops().values().stream()
                .sorted(Comparator.comparingInt(CropRecord::getId)).toList();
    }
}
