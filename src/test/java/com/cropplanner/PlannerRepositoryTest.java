package com.cropplanner;

import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Farmer;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import com.cropplanner.storage.DatabaseManager;
import com.cropplanner.storage.PlannerData;
import com.cropplanner.storage.PlannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs against a real (temporary) SQLite file. */
class PlannerRepositoryTest {

    private static final LocalDate SOWING = LocalDate.of(2026, 11, 5);

    private PlannerRepository repository;
    private final Worker worker = new Worker("Ramesh Kumar", "9988776655");

    @BeforeEach
    void setUp() throws IOException, SQLException {
        File file = File.createTempFile("planner-test", ".db");
        file.deleteOnExit();
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + file.getAbsolutePath());
        database.initialise();
        repository = new PlannerRepository(database);

        repository.insertFarm(new Farm(1, "Test Farm", "Patiala", new Farmer("Owner", "9876543210")));
        repository.insertCrop(new CropRecord(1, 1, "North Field", "wheat", 5.0, SOWING, SOWING.plusDays(120)));
    }

    @Test
    void savedRecordsAreLoadedBack() throws SQLException {
        repository.insertTasks(List.of(
                new Task(1, 1, ActivityType.IRRIGATION, SOWING.plusDays(21), worker, 3500)));

        PlannerData loaded = repository.load();

        assertEquals(1, loaded.getFarms().size());
        assertEquals("Wheat", loaded.getCrops().get(1).getCropType());
        assertEquals(1, loaded.getTasks().size());
        assertEquals("Ramesh Kumar", loaded.getTasks().get(0).getWorker().getName());
    }

    @Test
    void updatedTaskKeepsItsCompletionDetails() throws SQLException {
        Task task = new Task(1, 1, ActivityType.IRRIGATION, SOWING.plusDays(21), worker, 3500);
        repository.insertTasks(List.of(task));

        task.markCompleted(SOWING.plusDays(22), 3300);
        repository.updateTask(task);

        Task reloaded = repository.load().getTasks().get(0);
        assertTrue(reloaded.isCompleted());
        assertEquals(SOWING.plusDays(22), reloaded.getCompletedDate());
        assertEquals(3300.0, reloaded.getActualCost(), 0.001);
    }

    @Test
    void failedBatchInsertLeavesNothingBehind() throws SQLException {
        Task first = new Task(1, 1, ActivityType.IRRIGATION, SOWING.plusDays(21), worker, 3500);
        Task duplicateId = new Task(1, 1, ActivityType.FERTILIZER, SOWING.plusDays(25), worker, 9000);

        assertThrows(SQLException.class, () -> repository.insertTasks(List.of(first, duplicateId)));

        assertEquals(0, repository.load().getTasks().size());   // the first insert was rolled back
    }
}
