
package com.powerhub;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class EmergencyManager {

    public void addRequest(EmergencyRequest request)
            throws SQLException {

        String sql = """
            INSERT INTO emergency_requests
            (full_name, phone, location, emergency_type,
             description, priority, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, request.getFullName());
            ps.setString(2, request.getPhone());
            ps.setString(3, request.getLocation());
            ps.setString(4, request.getEmergencyType());
            ps.setString(5, request.getDescription());
            ps.setInt(6, request.getPriority());
            ps.setString(7, "PENDING");

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<EmergencyRequest> getAllRequests()
            throws SQLException {

        List<EmergencyRequest> requests = new ArrayList<>();

        String sql = """
            SELECT id, full_name, phone, location,
                   emergency_type, description, priority,
                   status, created_at
            FROM emergency_requests
            ORDER BY priority ASC, created_at ASC, id ASC
            """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                EmergencyRequest request =
                        new EmergencyRequest(
                                rs.getString("full_name"),
                                rs.getString("phone"),
                                rs.getString("location"),
                                rs.getString("emergency_type"),
                                rs.getString("description"),
                                rs.getInt("priority"));

                request.setId(rs.getInt("id"));
                request.setStatus(rs.getString("status"));

                Timestamp timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    request.setCreatedAt(
                            timestamp.toLocalDateTime());
                }

                requests.add(request);
            }
        }

        return requests;
    }

    public PriorityQueue<EmergencyRequest> getPriorityQueue()
            throws SQLException {

        PriorityQueue<EmergencyRequest> queue =
                new PriorityQueue<>();

        queue.addAll(getAllRequests());
        return queue;
    }

    public void updateStatus(int id, String status)
            throws SQLException {

        if (!List.of("PENDING", "IN PROGRESS", "RESOLVED")
                .contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid request status");
        }

        String sql =
                "UPDATE emergency_requests SET status = ? WHERE id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Request not found: " + id);
            }
        }
    }
}
