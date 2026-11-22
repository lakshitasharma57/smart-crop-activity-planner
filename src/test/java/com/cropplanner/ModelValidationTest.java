package com.cropplanner;

import com.cropplanner.exception.InvalidInputException;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelValidationTest {

    private static final LocalDate SOWING = LocalDate.of(2026, 11, 5);

    @Test
    void phoneNumberMustHaveTenDigits() {
        assertThrows(InvalidInputException.class, () -> new Worker("Ramesh", "12345"));
        assertThrows(InvalidInputException.class, () -> new Worker("Ramesh", "98765abcde"));
    }

    @Test
    void harvestMustBeAfterSowing() {
        assertThrows(InvalidInputException.class,
                () -> new CropRecord(1, 1, "North", "wheat", 5, SOWING, SOWING));
    }

    @Test
    void areaMustBePositive() {
        assertThrows(InvalidInputException.class,
                () -> new CropRecord(1, 1, "North", "wheat", 0, SOWING, SOWING.plusDays(100)));
    }

    @Test
    void cropTypeIsNormalised() {
        CropRecord crop = new CropRecord(1, 1, "North", "  wHEAT ", 2, SOWING, SOWING.plusDays(100));
        assertEquals("Wheat", crop.getCropType());
    }

    @Test
    void taskSurvivesCsvRoundTrip() {
        Task original = new Task(7, 2, com.cropplanner.model.ActivityType.IRRIGATION,
                SOWING, new Worker("Sunita Devi", "9123456780"), 3500);
        original.markCompleted(SOWING.plusDays(1), 3300);

        Task copy = Task.fromCsv(original.toCsv().split(",", -1));

        assertEquals(original.toCsv(), copy.toCsv());
        assertEquals(3300.0, copy.getActualCost(), 0.001);
    }
}
