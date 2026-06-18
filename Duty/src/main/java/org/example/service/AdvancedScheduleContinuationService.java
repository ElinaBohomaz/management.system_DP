package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class AdvancedScheduleContinuationService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public AdvancedScheduleContinuationService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    public void continueFebruaryFromPhoto(YearMonth february2026) throws SQLException {
        if (!february2026.equals(YearMonth.of(2026, 2))) {
            throw new IllegalArgumentException("Цей метод працює тільки для лютого 2026");
        }

        List<Employee> first10Employees = getFirst10Employees();
        List<Shift> allShifts = new ArrayList<>();
        Map<Integer, String[]> photoData = getPhotoDataFromImage();

        for (int i = 0; i < first10Employees.size(); i++) {
            Employee employee = first10Employees.get(i);
            String[] lastThreeDays = photoData.get(i);
            if (lastThreeDays != null) {
                List<Shift> februaryShifts = generateFebruaryShiftsFromPhoto(employee.getId(), february2026, i, lastThreeDays);
                allShifts.addAll(februaryShifts);
            }
        }

        if (!allShifts.isEmpty()) {
            shiftDAO.saveBatch(allShifts);
        }
    }

    private List<Employee> getFirst10Employees() throws SQLException {
        return employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .sorted(Comparator.comparing(Employee::getDepartment).thenComparing(Employee::getFullName))
                .limit(10)
                .toList();
    }

    private Map<Integer, String[]> getPhotoDataFromImage() {
        Map<Integer, String[]> data = new HashMap<>();
        data.put(0, new String[]{"X", "1", "2"});
        data.put(1, new String[]{"X", "1", "2"});
        data.put(2, new String[]{"X", "X", "1"});
        data.put(3, new String[]{"X", "X", "0"});
        data.put(4, new String[]{"2", "X", "X"});
        data.put(5, new String[]{"X", "X", "X"});
        data.put(6, new String[]{"X", "X", "X"});
        data.put(7, new String[]{"X", "X", "1"});
        data.put(8, new String[]{"X", "X", "1"});
        data.put(9, new String[]{"X", "1", "X"});
        return data;
    }

    private List<Shift> generateFebruaryShiftsFromPhoto(Integer employeeId, YearMonth februaryMonth, int employeeIndex, String[] lastThreeDays) {
        List<Shift> shifts = new ArrayList<>();
        int daysInFebruary = februaryMonth.lengthOfMonth();
        for (int day = 1; day <= daysInFebruary; day++) {
            LocalDate date = februaryMonth.atDay(day);
            String shiftCode;
            if (employeeIndex < 5) {
                if (day == 1) {
                    shiftCode = "X";
                } else {
                    shiftCode = continuePatternFromLastDays(day - 1, lastThreeDays);
                }
            } else {
                if (day <= 2) {
                    shiftCode = "X";
                } else {
                    shiftCode = continuePatternFromLastDays(day - 2, lastThreeDays);
                }
            }
            shifts.add(new Shift(employeeId, date, shiftCode));
        }
        return shifts;
    }

    private String continuePatternFromLastDays(int position, String[] lastThreeDays) {
        String day29 = lastThreeDays[0];
        String day30 = lastThreeDays[1];
        String day31 = lastThreeDays[2];

        if ("1".equals(day30) && "2".equals(day31)) {
            return switch (position % 3) {
                case 0 -> "X";
                case 1 -> "1";
                case 2 -> "2";
                default -> "X";
            };
        } else if ("X".equals(day30) && "1".equals(day31)) {
            return (position % 3 == 2) ? "1" : "X";
        } else if ("X".equals(day30) && "0".equals(day31)) {
            return (position % 3 == 2) ? "0" : "X";
        } else if ("2".equals(day29) && "X".equals(day30) && "X".equals(day31)) {
            return (position % 3 == 0) ? "2" : "X";
        } else if ("X".equals(day30) && "X".equals(day31)) {
            return "X";
        } else if ("X".equals(day29) && "1".equals(day30) && "X".equals(day31)) {
            return (position % 3 == 1) ? "1" : "X";
        }
        return "X";
    }
}