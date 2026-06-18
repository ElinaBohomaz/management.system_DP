package org.example.dao;

import org.example.model.Employee;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:duty_schedule.db");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер SQLite не знайдено", e);
        }
    }

    public void save(Employee employee) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO employees 
            (id, full_name, position, department, education, phone, 
             birth_date, hire_date, status, shift_type, days_off_after, 
             days_off_before, pattern_type, profkom, children, data,
             last_work_code, last_x_count, last_work_day)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, employee.getId());
            pstmt.setString(2, employee.getFullName());
            pstmt.setString(3, employee.getPosition());
            pstmt.setString(4, employee.getDepartment());
            pstmt.setString(5, employee.getEducation());
            pstmt.setString(6, employee.getPhone());

            if (employee.getBirthDate() != null) {
                pstmt.setDate(7, Date.valueOf(employee.getBirthDate()));
            } else {
                pstmt.setNull(7, Types.DATE);
            }

            if (employee.getHireDate() != null) {
                pstmt.setDate(8, Date.valueOf(employee.getHireDate()));
            } else {
                pstmt.setNull(8, Types.DATE);
            }

            pstmt.setString(9, employee.getStatus());
            pstmt.setString(10, employee.getShiftType());
            pstmt.setInt(11, employee.getDaysOffAfter());
            pstmt.setInt(12, employee.getDaysOffBefore());
            pstmt.setString(13, employee.getPatternType());

            pstmt.setString(14, employee.getProfkom());
            pstmt.setString(15, employee.getChildren());
            pstmt.setString(16, employee.getData());

            pstmt.setString(17, employee.getLastWorkCode());
            pstmt.setInt(18, employee.getLastXCount());
            pstmt.setInt(19, employee.getLastWorkDay());

            pstmt.executeUpdate();
        }
    }

    public Employee findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM employees WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        }

        return null;
    }

    public List<Employee> findAll() throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY department, full_name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        }

        return employees;
    }

    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM employees WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Employee> findByDepartment(String department) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE department = ? ORDER BY full_name";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, department);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        }

        return employees;
    }

    public List<Employee> findByStatus(String status) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE status = ? ORDER BY department, full_name";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        }

        return employees;
    }

    public List<String> findAllDepartments() throws SQLException {
        List<String> departments = new ArrayList<>();
        String sql = "SELECT DISTINCT department FROM employees WHERE department IS NOT NULL ORDER BY department";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                departments.add(rs.getString("department"));
            }
        }

        return departments;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();

        employee.setId(rs.getInt("id"));
        employee.setFullName(rs.getString("full_name"));
        employee.setPosition(rs.getString("position"));
        employee.setDepartment(rs.getString("department"));
        employee.setEducation(rs.getString("education"));
        employee.setPhone(rs.getString("phone"));

        Date birthDate = rs.getDate("birth_date");
        if (birthDate != null) {
            employee.setBirthDate(birthDate.toLocalDate());
        }

        Date hireDate = rs.getDate("hire_date");
        if (hireDate != null) {
            employee.setHireDate(hireDate.toLocalDate());
        }

        employee.setStatus(rs.getString("status"));
        employee.setShiftType(rs.getString("shift_type"));
        employee.setDaysOffAfter(rs.getInt("days_off_after"));
        employee.setDaysOffBefore(rs.getInt("days_off_before"));
        employee.setPatternType(rs.getString("pattern_type"));

        employee.setProfkom(rs.getString("profkom"));
        employee.setChildren(rs.getString("children"));
        employee.setData(rs.getString("data"));

        employee.setLastWorkCode(rs.getString("last_work_code"));
        employee.setLastXCount(rs.getInt("last_x_count"));
        employee.setLastWorkDay(rs.getInt("last_work_day"));

        return employee;
    }
}