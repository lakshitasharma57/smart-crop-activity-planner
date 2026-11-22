package com.cropplanner;

import com.cropplanner.exception.PlannerException;
import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farmer;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import com.cropplanner.service.FarmService;
import com.cropplanner.service.ReportService;
import com.cropplanner.service.ScheduleCatalog;
import com.cropplanner.service.TaskService;
import com.cropplanner.storage.PlannerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private static final LocalDate SOWING = LocalDate.of(2026, 11, 5);

    private PlannerData data;
    private TaskService tasks;
    private ReportService reports;
    private CropRecord wheat;
    private final Worker worker = new Worker("Ramesh Kumar", "9988776655");

    @BeforeEach
    void setUp() throws PlannerException {
        data = new PlannerData();
        FarmService farms = new FarmService(data);
        tasks = new TaskService(data, new ScheduleCatalog());
        reports = new ReportService(data);
        farms.registerFarm("Test Farm", "Patiala", new Farmer("Owner", "9876543210"));
        wheat = farms.addCrop(1, "North Field", "wheat", 5.0, SOWING, SOWING.plusDays(50));
    }

    @Test
    void scheduleSkipsStepsAfterHarvestDate() throws PlannerException {
        // wheat steps on day 0, 21, 25, 45 fit inside 50 days, plus the harvest itself
        List<Task> created = tasks.generateSchedule(wheat, worker);
        assertEquals(5, created.size());
        assertTrue(created.stream().noneMatch(t -> t.getScheduledDate().isAfter(wheat.getExpectedHarvestDate())));
    }

    @Test
    void scheduleCannotBeGeneratedTwice() throws PlannerException {
        tasks.generateSchedule(wheat, worker);
        assertThrows(PlannerException.class, () -> tasks.generateSchedule(wheat, worker));
    }

    @Test
    void estimatedCostDependsOnAreaAndActivity() throws PlannerException {
        Task irrigation = tasks.addTask(wheat, ActivityType.IRRIGATION, SOWING.plusDays(10), worker);
        assertEquals(3500.0, irrigation.getEstimatedCost(), 0.001);   // 700 per acre x 5 acres
    }

    @Test
    void onlyPendingPastDueTasksAreOverdue() throws PlannerException {
        List<Task> created = tasks.generateSchedule(wheat, worker);
        LocalDate today = SOWING.plusDays(30);                        // day 0, 21 and 25 are past due
        assertEquals(3, tasks.getOverdue(today).size());

        tasks.completeTask(created.get(0).getId(), today, 9000);
        assertEquals(2, tasks.getOverdue(today).size());
    }

    @Test
    void historyIsInChronologicalOrder() throws PlannerException {
        List<Task> created = tasks.generateSchedule(wheat, worker);
        tasks.completeTask(created.get(1).getId(), SOWING.plusDays(22), 3000);
        tasks.completeTask(created.get(0).getId(), SOWING, 9000);

        List<Task> history = tasks.getHistory();
        assertEquals(2, history.size());
        assertEquals(SOWING, history.get(0).getCompletedDate());
        assertEquals(SOWING.plusDays(22), history.get(1).getCompletedDate());
    }

    @Test
    void completedTaskCannotBeChangedOrCompletedAgain() throws PlannerException {
        Task task = tasks.generateSchedule(wheat, worker).get(0);
        tasks.completeTask(task.getId(), SOWING, 9000);
        assertThrows(PlannerException.class, () -> tasks.completeTask(task.getId(), SOWING, 9000));
        assertThrows(PlannerException.class, () -> tasks.updateTask(task.getId(), SOWING.plusDays(1), null));
    }

    @Test
    void updateChangesDateAndWorker() throws PlannerException {
        Task task = tasks.generateSchedule(wheat, worker).get(1);
        tasks.updateTask(task.getId(), SOWING.plusDays(23), new Worker("Sunita Devi", "9123456780"));
        assertEquals(SOWING.plusDays(23), task.getScheduledDate());
        assertEquals("Sunita Devi", task.getWorker().getName());
    }

    @Test
    void nextTasksAreListedMostUrgentFirst() throws PlannerException {
        tasks.generateSchedule(wheat, worker);
        String report = reports.nextTasksReport(2);
        assertTrue(report.indexOf("2026-11-05") < report.indexOf("2026-11-26"));
        assertFalse(report.contains("2026-11-30"));            // only the two most urgent are shown
    }

    @Test
    void supportedCropsAreSortedAndUnique() {
        assertEquals("cotton, maize, rice, wheat", String.join(", ", new ScheduleCatalog().getSupportedCrops()));
    }

    @Test
    void costReportSeparatesSpentFromPlanned() throws PlannerException {
        Task first = tasks.generateSchedule(wheat, worker).get(0);
        tasks.completeTask(first.getId(), SOWING, 9200);
        String report = reports.fieldCostReport();
        assertTrue(report.contains("Test Farm / North Field"));
        assertTrue(report.contains("9200"));
        assertFalse(report.contains("No crop records"));
    }
}
