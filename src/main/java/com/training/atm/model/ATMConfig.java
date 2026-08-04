package com.training.atm.model;

/**
 * Represents ATM configuration metadata (location and branch name).
 * Mirrors the single-row atm_info table.
 */
public class ATMConfig implements Identifiable<Integer> {
    private Integer id;
    private String location;
    private String branchName;

    public ATMConfig(Integer id, String location, String branchName) {
        this.id = id;
        this.location = location;
        this.branchName = branchName;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @Override
    public String toString() {
        return "ATMConfig{" +
                "id=" + id +
                ", location='" + location + '\'' +
                ", branchName='" + branchName + '\'' +
                '}';
    }
}
