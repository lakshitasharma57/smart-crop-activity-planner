package com.cropplanner;

import com.cropplanner.concurrent.OverdueMonitor;
import com.cropplanner.exception.PlannerException;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farmer;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import com.cropplanner.service.FarmService;
import com.cropplanner.service.ScheduleCatalog;
import com.cropplanner.service.TaskService;
import com.cropplanner.storage.PlannerData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverdueMonitorTest {

    private static final LocalDate SOWING = LocalDate.of(2026, 11, 5);

    @Test
    void countFollowsTheClockAndCompletedWork() throws PlannerException {
        PlannerData data = new PlannerData();
        FarmService farms = new FarmService(data);
        TaskService tasks = new TaskService(data, new ScheduleCatalog());
        farms.registerFarm("Test Farm", "Patiala", new Farmer("Owner", "9876543210"));
        CropRecord wheat = farms.addCrop(1, "North", "wheat", 5, SOWING, SOWING.plusDays(120));
        List<Task> created = tasks.generateSchedule(wheat, new Worker("Ramesh", "9988776655"));

        LocalDate[] now = {SOWING};                          // a clock the test can move
        OverdueMonitor monitor = new OverdueMonitor(tasks, () -> now[0], 1000);

        monitor.refresh();
        assertEquals(0, monitor.getOverdueCount());

        now[0] = SOWING.plusDays(30);                        // day 0, 21 and 25 are now past due
        monitor.refresh();
        assertEquals(3, monitor.getOverdueCount());

        tasks.completeTask(created.get(0).getId(), now[0], 9000);
        monitor.refresh();
        assertEquals(2, monitor.getOverdueCount());
    }

    @Test
    void backgroundThreadStartsAndStopsCleanly() throws PlannerException {
        PlannerData data = new PlannerData();
        TaskService tasks = new TaskService(data, new ScheduleCatalog());
        OverdueMonitor monitor = new OverdueMonitor(tasks, LocalDate::now, 10);
        assertEquals(Thread.State.NEW, monitor.getState());
        monitor.start();
        monitor.stop();                                      // must return, not hang
        assertEquals(Thread.State.TERMINATED, monitor.getState());
        assertEquals(0, monitor.getOverdueCount());
    }
}
