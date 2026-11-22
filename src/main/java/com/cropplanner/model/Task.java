package com.cropplanner.model;

import com.cropplanner.exception.InvalidInputException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/** A scheduled piece of farm work with a responsible worker. */
public class Task implements CsvSerializable {

    /** Earliest due date first; the id breaks ties. */
    public static final Comparator<Task> BY_SCHEDULE =
            Comparator.comparing(Task::getScheduledDate).thenComparingInt(Task::getId);

    private final int id;
    private final int cropId;
    private final ActivityType type;
    private LocalDate scheduledDate;
    private Worker worker;
    private final double estimatedCost;
    private TaskStatus status;
    private LocalDate completedDate;
    private double actualCost;

    public Task(int id, int cropId, ActivityType type, LocalDate scheduledDate,
                Worker worker, double estimatedCost) {
        if (type == null || scheduledDate == null || worker == null) {
            throw new InvalidInputException("Activity type, date and worker are required");
        }
        this.id = id;
        this.cropId = cropId;
        this.type = type;
        this.scheduledDate = scheduledDate;
        this.worker = worker;
        this.estimatedCost = estimatedCost;
        this.status = TaskStatus.PENDING;
    }

    public static Task fromCsv(String[] p) {
        if (p.length < 10) {
            throw new InvalidInputException("expected 10 fields but found " + p.length);
        }
        Task task = new Task(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
                ActivityType.valueOf(p[2].trim()), LocalDate.parse(p[3].trim()),
                new Worker(p[4], p[5]), Double.parseDouble(p[6].trim()));
        if (TaskStatus.valueOf(p[7].trim()) == TaskStatus.COMPLETED) {
            task.markCompleted(LocalDate.parse(p[8].trim()), Double.parseDouble(p[9].trim()));
        }
        return task;
    }

    @Override
    public String toCsv() {
        return String.join(",", String.valueOf(id), String.valueOf(cropId), type.name(),
                scheduledDate.toString(), CsvSerializable.clean(worker.getName()), worker.getPhone(),
                String.valueOf(estimatedCost), status.name(),
                completedDate == null ? "" : completedDate.toString(), String.valueOf(actualCost));
    }

    public void markCompleted(LocalDate date, double cost) {
        if (date == null) {
            throw new InvalidInputException("Completion date is required");
        }
        if (cost < 0) {
            throw new InvalidInputException("Cost cannot be negative");
        }
        this.status = TaskStatus.COMPLETED;
        this.completedDate = date;
        this.actualCost = cost;
    }

    public void reschedule(LocalDate newDate) {
        if (newDate == null) {
            throw new InvalidInputException("Date is required");
        }
        this.scheduledDate = newDate;
    }

    public void assignWorker(Worker newWorker) {
        if (newWorker == null) {
            throw new InvalidInputException("Worker is required");
        }
        this.worker = newWorker;
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    public boolean isOverdue(LocalDate today) {
        return status == TaskStatus.PENDING && scheduledDate.isBefore(today);
    }

    public long daysOverdue(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(scheduledDate, today) : 0;
    }

    public int getId() { return id; }
    public int getCropId() { return cropId; }
    public ActivityType getType() { return type; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public Worker getWorker() { return worker; }
    public double getEstimatedCost() { return estimatedCost; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getCompletedDate() { return completedDate; }
    public double getActualCost() { return actualCost; }
}
