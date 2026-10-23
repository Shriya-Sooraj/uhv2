
package com.powerhub;

import java.time.LocalDateTime;

public class EmergencyRequest
        implements Comparable<EmergencyRequest> {

    private int id;
    private String fullName;
    private String phone;
    private String location;
    private String emergencyType;
    private String description;
    private int priority;
    private String status;
    private LocalDateTime createdAt;

    public EmergencyRequest(
            String fullName,
            String phone,
            String location,
            String emergencyType,
            String description,
            int priority) {

        this.fullName = fullName;
        this.phone = phone;
        this.location = location;
        this.emergencyType = emergencyType;
        this.description = description;
        this.priority = priority;
        this.status = "PENDING";
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getEmergencyType() { return emergencyType; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int compareTo(EmergencyRequest other) {
        int result = Integer.compare(
                this.priority, other.priority);

        if (result != 0) {
            return result;
        }

        // For equal priorities, older requests come first.
        if (this.createdAt == null || other.createdAt == null) {
            return Integer.compare(this.id, other.id);
        }

        return this.createdAt.compareTo(other.createdAt);
    }
}
