package com.cropplanner;

import com.cropplanner.concurrent.OverdueMonitor;
import com.cropplanner.service.FarmService;
import com.cropplanner.service.ReportService;
import com.cropplanner.service.ScheduleCatalog;
import com.cropplanner.service.TaskService;
import com.cropplanner.storage.DatabaseManager;
import com.cropplanner.storage.PlannerData;
import com.cropplanner.storage.PlannerRepository;
import com.cropplanner.ui.ConsoleMenu;
import com.cropplanner.ui.InputReader;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {

    private static final String DB_URL = "jdbc:sqlite:data/planner.db";
    private static final long MONITOR_INTERVAL_MILLIS = 5000;

    public static void main(String[] args) {
        new File("data").mkdirs();                          // folder for the database file
        DatabaseManager database = new DatabaseManager(DB_URL);

        try (InputReader input = new InputReader()) {
            database.initialise();
            PlannerRepository repository = new PlannerRepository(database);
            PlannerData data = repository.load();

            ScheduleCatalog catalog = new ScheduleCatalog();
            TaskService tasks = new TaskService(data, catalog);
            OverdueMonitor monitor = new OverdueMonitor(tasks, LocalDate::now, MONITOR_INTERVAL_MILLIS);
            monitor.start();
            try {
                new ConsoleMenu(new FarmService(data), tasks, new ReportService(data), catalog,
                        repository, monitor, data, input, LocalDate.now()).run();
            } finally {
                monitor.stop();
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.err.println("If this says 'No suitable driver', the SQLite JDBC jar is missing from the build path.");
        }
    }
}
