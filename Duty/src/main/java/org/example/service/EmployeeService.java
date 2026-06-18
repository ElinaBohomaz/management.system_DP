package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.model.Employee;

import java.sql.SQLException;
import java.util.List;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
    }

    public void saveEmployee(Employee employee) throws SQLException {
        if (employee.getId() == null || employee.getId() == 0) {
            List<Employee> all = employeeDAO.findAll();
            int maxId = all.stream().mapToInt(Employee::getId).max().orElse(0);
            employee.setId(maxId + 1);
        }
        employeeDAO.save(employee);
    }

    public void deleteEmployee(Integer id) throws SQLException {
        employeeDAO.delete(id);
    }

    public Employee getEmployeeById(Integer id) throws SQLException {
        return employeeDAO.findById(id);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return employeeDAO.findAll();
    }

    public List<Employee> getEmployeesByDepartment(String department) throws SQLException {
        return employeeDAO.findByDepartment(department);
    }

    public List<String> getAllDepartments() throws SQLException {
        List<String> allDepartments = employeeDAO.findAllDepartments();
        allDepartments.removeIf(dept -> dept == null || dept.trim().isEmpty());
        return allDepartments;
    }

    public List<Employee> getEmployeesByStatus(String status) throws SQLException {
        return employeeDAO.findByStatus(status);
    }

    public List<Employee> getActiveEmployees() throws SQLException {
        return employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .toList();
    }
}