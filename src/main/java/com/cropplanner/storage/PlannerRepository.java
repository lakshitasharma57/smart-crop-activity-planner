package com.cropplanner.storage;

import com.cropplanner.model.ActivityType;
import com.cropplanner.model.CropRecord;
import com.cropplanner.model.Farm;
import com.cropplanner.model.Farmer;
import com.cropplanner.model.Task;
import com.cropplanner.model.TaskStatus;
import com.cropplanner.model.Worker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * JDBC persistence: reads everything into {@link PlannerData} at start-up and
 * writes each change with PreparedStatements. Every write runs in a transaction.
 */
public class PlannerRepository {

    private static final String INSERT_FARM =
            "INSERT INTO farms (id, name, location, owner_name, owner_phone) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_CROP =
            "INSERT INTO crops (id, farm_id, field_name, crop_type, area_acres, sowing_date, "
                    + "expected_harvest_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_TASK =
            "INSERT INTO tasks (id, crop_id, type, scheduled_date, worker_name, worker_phone, "
                    + "estimated_cost, status, completed_date, actual_cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TASK =
            "UPDATE tasks SET scheduled_date = ?, worker_name = ?, worker_phone = ?, status = ?, "
                    + "completed_date = ?, actual_cost = ? WHERE id = ?";

    /** A block of JDBC work that receives an open connection. */
    @FunctionalInterface
    private interface SqlWork {
        void run(Connection con) throws SQLException;
    }

    private final DatabaseManager database;

    public PlannerRepository(DatabaseManager database) {
        this.database = database;
    }

    // ---------- Read ----------

    public PlannerData load() throws SQLException {
        PlannerData data = new PlannerData();
        try (Connection con = database.getConnection(); Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM farms ORDER BY id")) {
                while (rs.next()) {
                    Farm farm = mapFarm(rs);
                    data.getFarms().put(farm.getId(), farm);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT * FROM crops ORDER BY id")) {
                while (rs.next()) {
                    CropRecord crop = mapCrop(rs);
                    data.getCrops().put(crop.getId(), crop);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT * FROM tasks ORDER BY id")) {
                while (rs.next()) {
                    data.getTasks().add(mapTask(rs));
                }
            }
        }
        return data;
    }

    // ---------- Create / Update ----------

    public void insertFarm(Farm farm) throws SQLException {
        inTransaction(con -> insertFarmRow(con, farm));
    }

    public void insertCrop(CropRecord crop) throws SQLException {
        inTransaction(con -> insertCropRow(con, crop));
    }

    /** All tasks are saved together or not at all. */
    public void insertTasks(List<Task> tasks) throws SQLException {
        inTransaction(con -> {
            for (Task task : tasks) {
                insertTaskRow(con, task);
            }
        });
    }

    public void updateTask(Task task) throws SQLException {
        inTransaction(con -> {
            try (PreparedStatement ps = con.prepareStatement(UPDATE_TASK)) {
                ps.setString(1, task.getScheduledDate().toString());
                ps.setString(2, task.getWorker().getName());
                ps.setString(3, task.getWorker().getPhone());
                ps.setString(4, task.getStatus().name());
                ps.setString(5, task.getCompletedDate() == null ? null : task.getCompletedDate().toString());
                ps.setDouble(6, task.getActualCost());
                ps.setInt(7, task.getId());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Task " + task.getId() + " does not exist in the database");
                }
            }
        });
    }

    /** Used by the CSV import: farms first, then crops, then tasks (foreign-key order). */
    public void insertAll(PlannerData data) throws SQLException {
        inTransaction(con -> {
            for (Farm farm : data.getFarms().values()) {
                insertFarmRow(con, farm);
            }
            for (CropRecord crop : data.getCrops().values()) {
                insertCropRow(con, crop);
            }
            for (Task task : data.getTasks()) {
                insertTaskRow(con, task);
            }
        });
    }

    // ---------- Row helpers ----------

    private void insertFarmRow(Connection con, Farm farm) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_FARM)) {
            ps.setInt(1, farm.getId());
            ps.setString(2, farm.getName());
            ps.setString(3, farm.getLocation());
            ps.setString(4, farm.getOwner().getName());
            ps.setString(5, farm.getOwner().getPhone());
            ps.executeUpdate();
        }
    }

    private void insertCropRow(Connection con, CropRecord crop) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_CROP)) {
            ps.setInt(1, crop.getId());
            ps.setInt(2, crop.getFarmId());
            ps.setString(3, crop.getFieldName());
            ps.setString(4, crop.getCropType());
            ps.setDouble(5, crop.getAreaAcres());
            ps.setString(6, crop.getSowingDate().toString());
            ps.setString(7, crop.getExpectedHarvestDate().toString());
            ps.executeUpdate();
        }
    }

    private void insertTaskRow(Connection con, Task task) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_TASK)) {
            ps.setInt(1, task.getId());
            ps.setInt(2, task.getCropId());
            ps.setString(3, task.getType().name());
            ps.setString(4, task.getScheduledDate().toString());
            ps.setString(5, task.getWorker().getName());
            ps.setString(6, task.getWorker().getPhone());
            ps.setDouble(7, task.getEstimatedCost());
            ps.setString(8, task.getStatus().name());
            ps.setString(9, task.getCompletedDate() == null ? null : task.getCompletedDate().toString());
            ps.setDouble(10, task.getActualCost());
            ps.executeUpdate();
        }
    }

    // ---------- ResultSet -> object mapping ----------

    private Farm mapFarm(ResultSet rs) throws SQLException {
        return new Farm(rs.getInt("id"), rs.getString("name"), rs.getString("location"),
                new Farmer(rs.getString("owner_name"), rs.getString("owner_phone")));
    }

    private CropRecord mapCrop(ResultSet rs) throws SQLException {
        return new CropRecord(rs.getInt("id"), rs.getInt("farm_id"), rs.getString("field_name"),
                rs.getString("crop_type"), rs.getDouble("area_acres"),
                LocalDate.parse(rs.getString("sowing_date")),
                LocalDate.parse(rs.getString("expected_harvest_date")));
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task(rs.getInt("id"), rs.getInt("crop_id"),
                ActivityType.valueOf(rs.getString("type")),
                LocalDate.parse(rs.getString("scheduled_date")),
                new Worker(rs.getString("worker_name"), rs.getString("worker_phone")),
                rs.getDouble("estimated_cost"));
        if (TaskStatus.valueOf(rs.getString("status")) == TaskStatus.COMPLETED) {
            task.markCompleted(LocalDate.parse(rs.getString("completed_date")), rs.getDouble("actual_cost"));
        }
        return task;
    }

    // ---------- Transaction handling ----------

    private void inTransaction(SqlWork work) throws SQLException {
        try (Connection con = database.getConnection()) {
            con.setAutoCommit(false);
            try {
                work.run(con);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
