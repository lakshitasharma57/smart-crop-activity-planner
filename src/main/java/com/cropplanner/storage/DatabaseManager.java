package com.cropplanner.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Owns the JDBC URL, opens connections and creates the tables. */
public class DatabaseManager {

    private final String url;

    /** @param url e.g. "jdbc:sqlite:data/planner.db" (the driver registers itself automatically) */
    public DatabaseManager(String url) {
        this.url = url;
    }

    public Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(url);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");          // SQLite ignores foreign keys unless asked
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }

    public void initialise() throws SQLException {
        String farms = "CREATE TABLE IF NOT EXISTS farms ("
                + "id INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL UNIQUE, "
                + "location TEXT NOT NULL, "
                + "owner_name TEXT NOT NULL, "
                + "owner_phone TEXT NOT NULL)";
        String crops = "CREATE TABLE IF NOT EXISTS crops ("
                + "id INTEGER PRIMARY KEY, "
                + "farm_id INTEGER NOT NULL REFERENCES farms(id), "
                + "field_name TEXT NOT NULL, "
                + "crop_type TEXT NOT NULL, "
                + "area_acres REAL NOT NULL CHECK (area_acres > 0), "
                + "sowing_date TEXT NOT NULL, "
                + "expected_harvest_date TEXT NOT NULL)";
        String tasks = "CREATE TABLE IF NOT EXISTS tasks ("
                + "id INTEGER PRIMARY KEY, "
                + "crop_id INTEGER NOT NULL REFERENCES crops(id), "
                + "type TEXT NOT NULL, "
                + "scheduled_date TEXT NOT NULL, "
                + "worker_name TEXT NOT NULL, "
                + "worker_phone TEXT NOT NULL, "
                + "estimated_cost REAL NOT NULL, "
                + "status TEXT NOT NULL CHECK (status IN ('PENDING', 'COMPLETED')), "
                + "completed_date TEXT, "
                + "actual_cost REAL NOT NULL DEFAULT 0)";
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(farms);
            st.executeUpdate(crops);
            st.executeUpdate(tasks);
        }
    }
}
