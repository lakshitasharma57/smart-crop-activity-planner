package com.cropplanner.service;

import com.cropplanner.exception.InvalidInputException;
import com.cropplanner.exception.PlannerException;
import com.cropplanner.exception.RecordNotFoundException;
import com.cropplanner.model.ActivityTemplate;
import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Task;
import com.cropplanner.model.Worker;
import com.cropplanner.storage.PlannerData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Creates, updates and completes tasks; finds overdue work and the work history.
 * Methods that touch the task list are synchronized because the background
 * {@code OverdueMonitor} thread reads the same list.
 */
public class TaskService {

    private final PlannerData data;
    private final ScheduleCatalog catalog;

    public TaskService(PlannerData data, ScheduleCatalog catalog) {
        this.data = data;
        this.catalog = catalog;
    }

    /** Builds every task of the crop's predefined schedule that falls before harvest. */
    public synchronized List<Task> generateSchedule(CropRecord crop, Worker worker) throws PlannerException {
        boolean alreadyPlanned = data.getTasks().stream().anyMatch(t -> t.getCropId() == crop.getId());
        if (alreadyPlanned) {
            throw new PlannerException("Crop " + crop.getId() + " already has scheduled tasks");
        }
        List<Task> created = new ArrayList<>();
        for (ActivityTemplate step : catalog.getSchedule(crop.getCropType())) {
            LocalDate due = step.dueDateFor(crop);
            if (!due.isAfter(crop.getExpectedHarvestDate())) {
                created.add(createTask(crop, step.getType(), due, worker));
            }
        }
        return created;
    }

    public synchronized Task addTask(CropRecord crop, ActivityType type, LocalDate date, Worker worker) {
        if (date.isBefore(crop.getSowingDate()) || date.isAfter(crop.getExpectedHarvestDate())) {
            throw new InvalidInputException("Task date must be between sowing and harvest dates");
        }
        return createTask(crop, type, date, worker);
    }

    /** A null date or worker means "leave unchanged". */
    public synchronized Task updateTask(int taskId, LocalDate newDate, Worker newWorker) throws PlannerException {
        Task task = findPending(taskId);
        if (newDate != null) {
            task.reschedule(newDate);
        }
        if (newWorker != null) {
            task.assignWorker(newWorker);
        }
        return task;
    }

    public synchronized Task completeTask(int taskId, LocalDate completedOn, double actualCost) throws PlannerException {
        Task task = findPending(taskId);
        task.markCompleted(completedOn, actualCost);
        return task;
    }

    public synchronized List<Task> getOverdue(LocalDate today) {
        return data.getTasks().stream()
                .filter(t -> t.isOverdue(today))
                .sorted(Task.BY_SCHEDULE)
                .toList();
    }

    /** Completed work in the order it was done. */
    public synchronized List<Task> getHistory() {
        return data.getTasks().stream()
                .filter(Task::isCompleted)
                .sorted(Comparator.comparing(Task::getCompletedDate).thenComparingInt(Task::getId))
                .toList();
    }

    /** Adds tasks that were imported from CSV (kept here so the lock covers every change to the list). */
    public synchronized void addAll(List<Task> imported) {
        data.getTasks().addAll(imported);
    }

    private Task createTask(CropRecord crop, ActivityType type, LocalDate date, Worker worker) {
        Task task = new Task(data.nextTaskId(), crop.getId(), type, date, worker,
                type.estimateCost(crop.getAreaAcres()));
        data.getTasks().add(task);
        return task;
    }

    private Task findPending(int taskId) throws PlannerException {
        Task task = data.getTasks().stream()
                .filter(t -> t.getId() == taskId)
                .findFirst()
                .orElseThrow(() -> new RecordNotFoundException("Task", taskId));
        if (task.isCompleted()) {
            throw new PlannerException("Task " + taskId + " is already completed and cannot be changed");
        }
        return task;
    }
}
