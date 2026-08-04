package com.training.atm.model;

import java.time.LocalDateTime;

/**
 * Represents an admin audit log entry.
 * Mirrors the append-only admin_log.txt file structure.
 */
public class AdminLog implements Identifiable<Long> {
    private Long id;
    private String logTime;
    private String adminUser;
    private String action;

    public AdminLog(Long id, String logTime, String adminUser, String action) {
        this.id = id;
        this.logTime = logTime;
        this.adminUser = adminUser;
        this.action = action;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "AdminLog{" +
                "id=" + id +
                ", logTime='" + logTime + '\'' +
                ", adminUser='" + adminUser + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
