package org.example.service;

public class EmployeeFullInfo {
    private final Integer id;
    private final String pib;
    private final String position;
    private final String department;
    private final String education;
    private final String phone;
    private final String birthDate;
    private final String hireDate;
    private final String profkom;
    private final String children;
    private final String data;
    private String status;

    public EmployeeFullInfo(Integer id, String pib, String position, String department,
                            String education, String phone, String birthDate,
                            String hireDate, String profkom, String children, String data) {
        this.id = id;
        this.pib = pib;
        this.position = position;
        this.department = department;
        this.education = education;
        this.phone = phone;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
        this.profkom = profkom;
        this.children = children;
        this.data = data;
        this.status = "працює";
    }

    public Integer getId() { return id; }
    public String getPib() { return pib; }
    public String getPosition() { return position; }
    public String getDepartment() { return department; }
    public String getEducation() { return education; }
    public String getPhone() { return phone; }
    public String getBirthDate() { return birthDate; }
    public String getHireDate() { return hireDate; }
    public String getProfkom() { return profkom; }
    public String getChildren() { return children; }
    public String getData() { return data; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}