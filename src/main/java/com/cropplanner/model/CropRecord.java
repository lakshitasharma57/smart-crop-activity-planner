package com.cropplanner.model;

import com.cropplanner.exception.InvalidInputException;

import java.time.LocalDate;

/** One crop grown on one field of a farm during one season. */
public class CropRecord implements CsvSerializable {

    private final int id;
    private final int farmId;
    private final String fieldName;
    private final String cropType;
    private final double areaAcres;
    private final LocalDate sowingDate;
    private final LocalDate expectedHarvestDate;

    public CropRecord(int id, int farmId, String fieldName, String cropType,
                      double areaAcres, LocalDate sowingDate, LocalDate expectedHarvestDate) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new InvalidInputException("Field name cannot be empty");
        }
        if (cropType == null || cropType.isBlank()) {
            throw new InvalidInputException("Crop type cannot be empty");
        }
        if (areaAcres <= 0) {
            throw new InvalidInputException("Field area must be greater than zero");
        }
        if (sowingDate == null || expectedHarvestDate == null) {
            throw new InvalidInputException("Sowing and harvest dates are required");
        }
        if (!expectedHarvestDate.isAfter(sowingDate)) {
            throw new InvalidInputException("Harvest date must be after the sowing date");
        }
        this.id = id;
        this.farmId = farmId;
        this.fieldName = fieldName.trim();
        this.cropType = capitalise(cropType.trim());
        this.areaAcres = areaAcres;
        this.sowingDate = sowingDate;
        this.expectedHarvestDate = expectedHarvestDate;
    }

    /** "wHEAT" -> "Wheat" */
    private static String capitalise(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    public static CropRecord fromCsv(String[] p) {
        if (p.length < 7) {
            throw new InvalidInputException("expected 7 fields but found " + p.length);
        }
        return new CropRecord(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), p[2], p[3],
                Double.parseDouble(p[4].trim()), LocalDate.parse(p[5].trim()), LocalDate.parse(p[6].trim()));
    }

    @Override
    public String toCsv() {
        return String.join(",", String.valueOf(id), String.valueOf(farmId), CsvSerializable.clean(fieldName),
                cropType, String.valueOf(areaAcres), sowingDate.toString(), expectedHarvestDate.toString());
    }

    public int getId() { return id; }
    public int getFarmId() { return farmId; }
    public String getFieldName() { return fieldName; }
    public String getCropType() { return cropType; }
    public double getAreaAcres() { return areaAcres; }
    public LocalDate getSowingDate() { return sowingDate; }
    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }

    @Override
    public String toString() {
        return String.format("Crop #%d  %-10s %-12s %5.1f acres  farm #%d  sown %s  harvest %s",
                id, cropType, fieldName, areaAcres, farmId, sowingDate, expectedHarvestDate);
    }
}
