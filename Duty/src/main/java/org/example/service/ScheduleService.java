package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ScheduleService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public ScheduleService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    public Map<Employee, List<Shift>> loadScheduleForMonth(YearMonth month) throws SQLException {
        Map<Employee, List<Shift>> scheduleMap = new HashMap<>();
        Map<Integer, List<Shift>> shiftsByEmployee = shiftDAO.findShiftsForMonth(month);
        List<Employee> allEmployees = employeeDAO.findAll();
        Map<Integer, Employee> employeeMap = new HashMap<>();
        for (Employee emp : allEmployees) {
            employeeMap.put(emp.getId(), emp);
        }
        for (Map.Entry<Integer, List<Shift>> entry : shiftsByEmployee.entrySet()) {
            Employee employee = employeeMap.get(entry.getKey());
            if (employee != null && employee.isCurrentlyWorking()) {
                scheduleMap.put(employee, entry.getValue());
            }
        }
        for (Employee employee : allEmployees) {
            if (employee.isCurrentlyWorking() && !scheduleMap.containsKey(employee)) {
                scheduleMap.put(employee, new ArrayList<>());
            }
        }
        return scheduleMap;
    }

    public void saveShifts(List<Shift> shifts) throws SQLException {
        if (shifts == null || shifts.isEmpty()) {
            return;
        }
        shiftDAO.saveBatch(shifts);
    }

    public Map<LocalDate, List<Shift>> loadShiftsByDate(YearMonth month) throws SQLException {
        Map<LocalDate, List<Shift>> shiftsByDate = new HashMap<>();
        Map<Integer, List<Shift>> shiftsByEmployee = shiftDAO.findShiftsForMonth(month);
        for (List<Shift> employeeShifts : shiftsByEmployee.values()) {
            for (Shift shift : employeeShifts) {
                LocalDate date = shift.getDate();
                if (!shiftsByDate.containsKey(date)) {
                    shiftsByDate.put(date, new ArrayList<>());
                }
                shiftsByDate.get(date).add(shift);
            }
        }
        return shiftsByDate;
    }

    public int getWorkingDaysCountForEmployee(Integer employeeId, YearMonth month) throws SQLException {
        List<Shift> shifts = shiftDAO.findShiftsForEmployeeAndMonth(employeeId, month);
        return (int) shifts.stream()
                .filter(s -> "1".equals(s.getCode()) || "2".equals(s.getCode()) || "12".equals(s.getCode()))
                .count();
    }

    public double getTotalHoursForEmployee(Integer employeeId, YearMonth month) throws SQLException {
        List<Shift> shifts = shiftDAO.findShiftsForEmployeeAndMonth(employeeId, month);
        double total = 0.0;
        for (Shift shift : shifts) {
            String code = shift.getCode();
            if ("1".equals(code)) {
                total += 24.0;
            } else if ("2".equals(code) || "12".equals(code)) {
                total += 12.0;
            } else if ("8.25".equals(code)) {
                total += 8.25;
            } else if ("7.0".equals(code)) {
                total += 7.0;
            }
        }
        return total;
    }
}