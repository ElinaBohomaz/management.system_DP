package org.example.model;

import java.time.LocalDate;

public class Employee {
    private Integer id;
    private String fullName;
    private String position;
    private String department;
    private String education;
    private String phone;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private String status;
    private String shiftType;
    private Integer daysOffAfter;
    private Integer daysOffBefore;
    private String patternType;

    private String profkom;
    private String children;
    private String data;

    private String lastWorkCode;
    private Integer lastXCount;
    private Integer lastWorkDay;

    public Employee() {
        this.status = "працює";
        this.shiftType = "1";
        this.daysOffAfter = 1;
        this.daysOffBefore = 0;
        this.patternType = "1_3_1";
        this.profkom = "";
        this.children = "";
        this.data = "";
        this.lastWorkCode = "";
        this.lastXCount = 0;
        this.lastWorkDay = 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public Integer getDaysOffAfter() { return daysOffAfter; }
    public void setDaysOffAfter(Integer daysOffAfter) {
        this.daysOffAfter = daysOffAfter != null ? daysOffAfter : 1;
    }

    public Integer getDaysOffBefore() { return daysOffBefore; }
    public void setDaysOffBefore(Integer daysOffBefore) {
        this.daysOffBefore = daysOffBefore != null ? daysOffBefore : 0;
    }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) {
        this.patternType = patternType != null ? patternType : "1_3_1";
    }

    public String getProfkom() { return profkom; }
    public void setProfkom(String profkom) {
        this.profkom = profkom != null ? profkom : "";
    }

    public String getChildren() { return children; }
    public void setChildren(String children) {
        this.children = children != null ? children : "";
    }

    public String getData() { return data; }
    public void setData(String data) {
        this.data = data != null ? data : "";
    }

    public String getLastWorkCode() { return lastWorkCode; }
    public void setLastWorkCode(String lastWorkCode) {
        this.lastWorkCode = lastWorkCode != null ? lastWorkCode : "";
    }

    public Integer getLastXCount() { return lastXCount; }
    public void setLastXCount(Integer lastXCount) {
        this.lastXCount = lastXCount != null ? lastXCount : 0;
    }

    public Integer getLastWorkDay() { return lastWorkDay; }
    public void setLastWorkDay(Integer lastWorkDay) {
        this.lastWorkDay = lastWorkDay != null ? lastWorkDay : 0;
    }

    public boolean isCurrentlyWorking() {
        return "працює".equals(status);
    }

    @Override
    public String toString() {
        return fullName + " (" + department + ")";
    }
}