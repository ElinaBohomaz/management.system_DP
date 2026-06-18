package org.example.model;

import java.time.LocalDate;

public class Shift {
    private Integer id;
    private Integer employeeId;
    private LocalDate date;
    private String code;
    private String notes;

    public Shift() {}

    public Shift(Integer employeeId, LocalDate date, String code) {
        this.employeeId = employeeId;
        this.date = date;
        this.code = code;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ShiftStatus getStatus() {
        return ShiftStatus.fromCode(code);
    }

    public boolean isWorkingShift() {
        return ShiftHelper.isWorkingShift(this);
    }

    public boolean isSpecialShift() {
        return ShiftHelper.isSpecialShift(this);
    }

    public boolean isDayOff() {
        return "X".equals(code);
    }

    @Override
    public String toString() {
        return "Shift{" +
                "employeeId=" + employeeId +
                ", date=" + date +
                ", code='" + code + '\'' +
                '}';
    }
}