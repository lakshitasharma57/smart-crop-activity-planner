package com.cropplanner.ui;

import com.cropplanner.concurrent.OverdueMonitor;
import com.cropplanner.exception.InvalidInputException;
import com.cropplanner.exception.PlannerException;
import com.cropplanner.model.ActivityTemplate;
import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Farmer;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import com.cropplanner.service.FarmService;
import com.cropplanner.service.ReportService;
import com.cropplanner.service.ScheduleCatalog;
import com.cropplanner.service.TaskService;
import com.cropplanner.storage.CsvTransfer;
import com.cropplanner.storage.PlannerData;
import com.cropplanner.storage.PlannerRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Text menu connecting the user to the services. Every change is written to the database. */
public class ConsoleMenu {

    private static final String IMPORT_DIR = "data/sample";
    private static final String EXPORT_DIR = "data/export";

    private final FarmService farms;
    private final TaskService tasks;
    private final ReportService reports;
    private final ScheduleCatalog catalog;
    private final PlannerRepository repository;
    private final OverdueMonitor monitor;
    private final PlannerData data;
    private final InputReader in;
    private final LocalDate today;
    private final CsvTransfer csv = new CsvTransfer();

    public ConsoleMenu(FarmService farms, TaskService tasks, ReportService reports, ScheduleCatalog catalog,
                       PlannerRepository repository, OverdueMonitor monitor, PlannerData data,
                       InputReader in, LocalDate today) {
        this.farms = farms;
        this.tasks = tasks;
        this.reports = reports;
        this.catalog = catalog;
        this.repository = repository;
        this.monitor = monitor;
        this.data = data;
        this.in = in;
        this.today = today;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = in.readInt("Choose an option: ");
            try {
                running = handle(choice);
            } catch (PlannerException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (InvalidInputException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
            monitor.refresh();
        }
    }

    private boolean handle(int choice) throws PlannerException, IOException, SQLException {
        switch (choice) {
            case 1 -> registerFarm();
            case 2 -> printAll(farms.getFarms());
            case 3 -> addCrop();
            case 4 -> printAll(farms.getCrops());
            case 5 -> generateSchedule(farms.getCrop(in.readInt("Crop id: ")));
            case 6 -> addCustomTask();
            case 7 -> updateTask();
            case 8 -> completeTask();
            case 9 -> System.out.println(reports.upcomingTasksReport(today, in.readInt("Days ahead: ")));
            case 10 -> showOverdue();
            case 11 -> showHistory();
            case 12 -> System.out.println(reports.cropProgressReport());
            case 13 -> showCostSummary();
            case 14 -> showSchedules();
            case 15 -> importCsv();
            case 16 -> exportCsv();
            case 17 -> System.out.println(reports.nextTasksReport(5));
            case 18 -> System.out.println(monitor.describe());
            case 0 -> {
                System.out.println("Goodbye! All changes are already saved in the database.");
                return false;
            }
            default -> System.out.println("Unknown option.");
        }
        return true;
    }

    private void registerFarm() throws PlannerException, SQLException {
        String name = in.readLine("Farm name: ");
        String location = in.readLine("Location: ");
        Farmer owner = new Farmer(in.readLine("Owner name: "), in.readLine("Owner phone (10 digits): "));
        Farm farm = farms.registerFarm(name, location, owner);
        repository.insertFarm(farm);
        System.out.println("Registered: " + farm);
    }

    private void addCrop() throws PlannerException, SQLException {
        int farmId = in.readInt("Farm id: ");
        String fieldName = in.readLine("Field name: ");
        System.out.println("Crops with a standard schedule: " + String.join(", ", catalog.getSupportedCrops()));
        String cropType = in.readLine("Crop type: ");
        double area = in.readDouble("Field area (acres): ");
        LocalDate sowing = in.readDate("Sowing date (yyyy-MM-dd): ");
        LocalDate harvest = in.readDate("Expected harvest date (yyyy-MM-dd): ");

        CropRecord crop = farms.addCrop(farmId, fieldName, cropType, area, sowing, harvest);
        repository.insertCrop(crop);
        System.out.println("Added: " + crop);

        if (in.readLine("Generate the standard schedule now? (y/n): ").equalsIgnoreCase("y")) {
            generateSchedule(crop);
        }
    }

    private void generateSchedule(CropRecord crop) throws PlannerException, SQLException {
        System.out.println("Enter the worker responsible for these tasks (can be changed later).");
        Worker worker = new Worker(in.readLine("Worker name: "), in.readLine("Worker phone (10 digits): "));
        List<Task> created = tasks.generateSchedule(crop, worker);
        repository.insertTasks(created);
        System.out.println(created.size() + " tasks scheduled:");
        created.forEach(t -> System.out.println("  " + reports.taskLine(t)));
    }

    private void addCustomTask() throws PlannerException, SQLException {
        CropRecord crop = farms.getCrop(in.readInt("Crop id: "));
        ActivityType[] types = ActivityType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i]);
        }
        int pick = in.readInt("Activity type: ");
        if (pick < 1 || pick > types.length) {
            throw new InvalidInputException("Activity type must be between 1 and " + types.length);
        }
        LocalDate date = in.readDate("Scheduled date (yyyy-MM-dd): ");
        Worker worker = new Worker(in.readLine("Worker name: "), in.readLine("Worker phone (10 digits): "));
        Task task = tasks.addTask(crop, types[pick - 1], date, worker);
        repository.insertTasks(List.of(task));
        System.out.println("Created: " + reports.taskLine(task));
    }

    private void updateTask() throws PlannerException, SQLException {
        int id = in.readInt("Task id: ");
        LocalDate newDate = in.readOptionalDate("New date (yyyy-MM-dd, blank = keep): ");
        String name = in.readLine("New worker name (blank = keep): ");
        Worker worker = name.isEmpty() ? null : new Worker(name, in.readLine("Worker phone (10 digits): "));
        Task task = tasks.updateTask(id, newDate, worker);
        repository.updateTask(task);
        System.out.println("Updated: " + reports.taskLine(task));
    }

    private void completeTask() throws PlannerException, SQLException {
        int id = in.readInt("Task id: ");
        LocalDate date = in.readOptionalDate("Completion date (yyyy-MM-dd, blank = today): ");
        double cost = in.readDouble("Actual cost (Rs): ");
        Task task = tasks.completeTask(id, date == null ? today : date, cost);
        repository.updateTask(task);
        System.out.println("Completed: " + reports.taskLine(task));
    }

    private void showOverdue() {
        List<Task> overdue = tasks.getOverdue(today);
        System.out.println("---- Overdue activities (as of " + today + ") ----");
        if (overdue.isEmpty()) {
            System.out.println("Nothing is overdue.");
        }
        for (Task t : overdue) {
            System.out.println(reports.taskLine(t) + "  [" + t.daysOverdue(today) + " days late]");
        }
    }

    private void showHistory() {
        List<Task> history = tasks.getHistory();
        System.out.println("---- Completed work history ----");
        if (history.isEmpty()) {
            System.out.println("No completed work yet.");
        }
        history.forEach(t -> System.out.println(reports.taskLine(t)));
    }

    private void showCostSummary() {
        System.out.println(reports.fieldCostReport());
        System.out.println();
        System.out.println(reports.activityCostMatrix());
    }

    private void showSchedules() {
        for (String crop : catalog.getSupportedCrops()) {
            System.out.println(crop.toUpperCase() + ":");
            try {
                for (ActivityTemplate step : catalog.getSchedule(crop)) {
                    String when = step.getType() == ActivityType.HARVESTING
                            ? "on expected harvest date" : "day " + step.getDaysAfterSowing() + " after sowing";
                    System.out.println("   " + step.getType() + " - " + when);
                }
            } catch (PlannerException e) {
                System.out.println("   (unavailable)");
            }
        }
    }

    /** Loads the CSV files in data/sample into an empty database (all rows or none). */
    private void importCsv() throws PlannerException, IOException, SQLException {
        if (!data.getFarms().isEmpty() || !data.getCrops().isEmpty() || !data.getTasks().isEmpty()) {
            throw new PlannerException("Import is only allowed while the database is empty");
        }
        PlannerData imported = csv.importFrom(IMPORT_DIR);
        repository.insertAll(imported);                    // if this fails nothing is changed
        data.getFarms().putAll(imported.getFarms());
        data.getCrops().putAll(imported.getCrops());
        tasks.addAll(imported.getTasks());
        System.out.printf("Imported %d farms, %d crops and %d tasks from %s.%n",
                imported.getFarms().size(), imported.getCrops().size(), imported.getTasks().size(), IMPORT_DIR);
    }

    private void exportCsv() throws IOException {
        csv.exportTo(data, EXPORT_DIR);
        System.out.println("Data exported to the folder " + EXPORT_DIR);
    }

    private void printAll(List<?> items) {
        if (items.isEmpty()) {
            System.out.println("(nothing to show)");
        } else {
            items.forEach(System.out::println);
        }
    }

    private void printMenu() {
        System.out.println("""

                ===== Smart Crop Activity Planner =====
                 1. Register farm              9. Upcoming tasks
                 2. View farms                10. Overdue activities
                 3. Add crop record           11. Completed work history
                 4. View crop records         12. Crop-wise progress
                 5. Generate standard schedule 13. Field-wise cost summary
                 6. Add custom task           14. View standard schedules
                 7. Update task               15. Import sample CSV data
                 8. Complete a task           16. Export data to CSV
                17. Next 5 urgent tasks       18. Background monitor status
                 0. Exit
                """);
        int overdue = monitor.getOverdueCount();
        if (overdue > 0) {
            System.out.println(" !! " + overdue + " overdue activities - choose 10 to see them.");
        }
    }
}
