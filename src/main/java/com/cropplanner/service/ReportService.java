package com.cropplanner.service;

import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Task;
import com.cropplanner.storage.PlannerData;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Builds the text reports using the Stream API. */
public class ReportService {

    private final PlannerData data;

    public ReportService(PlannerData data) {
        this.data = data;
    }

    /** One formatted line describing a task, used by every list on screen. */
    public String taskLine(Task t) {
        CropRecord crop = data.getCrops().get(t.getCropId());
        String cropLabel = crop == null ? "crop " + t.getCropId()
                : crop.getCropType() + " - " + crop.getFieldName();
        String line = String.format("#%-3d %-22s %-24s due %s  worker: %s",
                t.getId(), t.getType(), cropLabel, t.getScheduledDate(), t.getWorker().getName());
        if (t.isCompleted()) {
            return line + String.format("  | done %s, cost Rs %.2f", t.getCompletedDate(), t.getActualCost());
        }
        return line + String.format("  | est. Rs %.2f", t.getEstimatedCost());
    }

    public String cropProgressReport() {
        List<CropRecord> crops = data.getCrops().values().stream()
                .sorted(Comparator.comparingInt(CropRecord::getId)).toList();
        if (crops.isEmpty()) {
            return "No crop records yet.";
        }
        StringBuilder sb = new StringBuilder("---- Crop-wise Activity Progress ----\n");
        for (CropRecord crop : crops) {
            List<Task> tasks = data.getTasks().stream()
                    .filter(t -> t.getCropId() == crop.getId()).toList();
            long done = tasks.stream().filter(Task::isCompleted).count();
            int percent = tasks.isEmpty() ? 0 : (int) (done * 100 / tasks.size());

            sb.append(String.format("Crop #%d  %s, %s (%.1f acres)%n",
                    crop.getId(), crop.getCropType(), fieldLabel(crop), crop.getAreaAcres()));
            sb.append(String.format("   [%-10s] %d%%  (%d of %d tasks done)%n",
                    "#".repeat(percent / 10), percent, done, tasks.size()));

            List<String> workers = tasks.stream()
                    .map(t -> t.getWorker().getName()).distinct().sorted().toList();
            if (!workers.isEmpty()) {
                sb.append("   Workers: ").append(String.join(", ", workers)).append('\n');
            }
            tasks.stream().filter(t -> !t.isCompleted()).sorted(Task.BY_SCHEDULE).limit(1)
                    .forEach(t -> sb.append("   Next: ").append(taskLine(t)).append('\n'));
        }
        return sb.toString().stripTrailing();
    }

    public String upcomingTasksReport(LocalDate today, int days) {
        LocalDate last = today.plusDays(days);
        List<Task> upcoming = data.getTasks().stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> !t.getScheduledDate().isBefore(today) && !t.getScheduledDate().isAfter(last))
                .sorted(Task.BY_SCHEDULE)
                .toList();
        StringBuilder sb = new StringBuilder(
                String.format("---- Upcoming tasks: %s to %s ----", today, last));
        if (upcoming.isEmpty()) {
            sb.append("\nNothing scheduled in this period.");
        }
        upcoming.forEach(t -> sb.append('\n').append(taskLine(t)));
        return sb.toString();
    }

    /** A PriorityQueue always hands out the pending task with the earliest due date (overdue ones first). */
    public String nextTasksReport(int count) {
        PriorityQueue<Task> queue = new PriorityQueue<>(Task.BY_SCHEDULE);
        for (Task t : data.getTasks()) {
            if (!t.isCompleted()) {
                queue.add(t);
            }
        }
        StringBuilder sb = new StringBuilder("---- Next " + count + " tasks to do (most urgent first) ----");
        if (queue.isEmpty()) {
            sb.append("\nNo pending tasks.");
        }
        for (int i = 0; i < count && !queue.isEmpty(); i++) {
            sb.append('\n').append(taskLine(queue.poll()));
        }
        return sb.toString();
    }

    /** Spent = actual cost of completed tasks; planned = estimate of pending tasks. */
    public String fieldCostReport() {
        Map<String, Double> spent = new HashMap<>();
        Map<String, Double> planned = new HashMap<>();
        for (Task t : data.getTasks()) {
            CropRecord crop = data.getCrops().get(t.getCropId());
            if (crop == null) {
                continue;
            }
            String field = fieldLabel(crop);
            if (t.isCompleted()) {
                spent.merge(field, t.getActualCost(), Double::sum);
            } else {
                planned.merge(field, t.getEstimatedCost(), Double::sum);
            }
        }
        List<String> fields = data.getCrops().values().stream()
                .map(this::fieldLabel).distinct().sorted().toList();
        if (fields.isEmpty()) {
            return "No crop records yet.";
        }
        StringBuilder sb = new StringBuilder("---- Field-wise Farming Cost (Rs) ----\n");
        sb.append(String.format("%-32s %12s %12s %12s%n", "Field", "Spent", "Planned", "Total"));
        double totalSpent = 0;
        double totalPlanned = 0;
        for (String field : fields) {
            double s = spent.getOrDefault(field, 0.0);
            double p = planned.getOrDefault(field, 0.0);
            totalSpent += s;
            totalPlanned += p;
            sb.append(String.format("%-32s %12.2f %12.2f %12.2f%n", field, s, p, s + p));
        }
        sb.append(String.format("%-32s %12.2f %12.2f %12.2f", "ALL FIELDS",
                totalSpent, totalPlanned, totalSpent + totalPlanned));
        return sb.toString();
    }

    /** Rows = fields, columns = activity types; completed tasks use actual cost, pending ones the estimate. */
    public String activityCostMatrix() {
        List<String> fields = data.getCrops().values().stream()
                .map(this::fieldLabel).distinct().sorted().toList();
        if (fields.isEmpty()) {
            return "No crop records yet.";
        }
        ActivityType[] types = ActivityType.values();
        double[][] cost = new double[fields.size()][types.length];
        for (Task t : data.getTasks()) {
            CropRecord crop = data.getCrops().get(t.getCropId());
            if (crop != null) {
                int row = fields.indexOf(fieldLabel(crop));
                cost[row][t.getType().ordinal()] += t.isCompleted() ? t.getActualCost() : t.getEstimatedCost();
            }
        }

        StringBuilder sb = new StringBuilder("---- Cost by Field and Activity (Rs) ----\n");
        sb.append(String.format("%-32s", "Field"));
        for (ActivityType type : types) {
            sb.append(String.format(" %12s", type.name()));
        }
        sb.append(String.format(" %12s%n", "TOTAL"));

        double[] columnTotals = new double[types.length];
        for (int r = 0; r < fields.size(); r++) {
            double rowTotal = 0;
            sb.append(String.format("%-32s", fields.get(r)));
            for (int c = 0; c < types.length; c++) {
                sb.append(String.format(" %12.2f", cost[r][c]));
                rowTotal += cost[r][c];
                columnTotals[c] += cost[r][c];
            }
            sb.append(String.format(" %12.2f%n", rowTotal));
        }
        double grandTotal = 0;
        sb.append(String.format("%-32s", "ALL FIELDS"));
        for (double columnTotal : columnTotals) {
            sb.append(String.format(" %12.2f", columnTotal));
            grandTotal += columnTotal;
        }
        sb.append(String.format(" %12.2f", grandTotal));
        return sb.toString();
    }

    private String fieldLabel(CropRecord crop) {
        Farm farm = data.getFarms().get(crop.getFarmId());
        String farmName = farm == null ? "Farm " + crop.getFarmId() : farm.getName();
        return farmName + " / " + crop.getFieldName();
    }
}
