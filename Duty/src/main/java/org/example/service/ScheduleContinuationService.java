package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ScheduleContinuationService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public ScheduleContinuationService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    public void continueFromDatabase(YearMonth targetMonth) throws SQLException {
        boolean hasExistingShifts = hasShiftsForMonth(targetMonth);
        if (hasExistingShifts) {
            return;
        }
        boolean isPreinitializedMonth = isPreinitializedMonth(targetMonth);
        if (isPreinitializedMonth) {
            ensureDataExistsForMonth(targetMonth);
        } else {
            generateNewScheduleFromPreviousMonth(targetMonth);
        }
    }

    public boolean hasShiftsForMonth(YearMonth month) throws SQLException {
        Map<Integer, List<Shift>> shifts = shiftDAO.findShiftsForMonth(month);
        return !shifts.isEmpty();
    }

    private boolean isPreinitializedMonth(YearMonth month) {
        return month.equals(YearMonth.of(2025, 12)) ||
                month.equals(YearMonth.of(2026, 1)) ||
                month.equals(YearMonth.of(2026, 2));
    }

    private void ensureDataExistsForMonth(YearMonth month) throws SQLException {
        Map<Integer, List<Shift>> shifts = shiftDAO.findShiftsForMonth(month);
        if (shifts.isEmpty()) {
            generateEmptySchedule(month);
        }
    }

    private void generateNewScheduleFromPreviousMonth(YearMonth targetMonth) throws SQLException {
        YearMonth previousMonth = targetMonth.minusMonths(1);
        boolean hasPreviousData = hasRealDataForMonth(previousMonth);
        if (!hasPreviousData) {
            generateEmptySchedule(targetMonth);
            return;
        }
        List<Employee> employees = employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .sorted(Comparator.comparing(Employee::getDepartment).thenComparing(Employee::getFullName))
                .toList();
        if (employees.isEmpty()) {
            return;
        }
        List<Shift> newShifts = new ArrayList<>();
        for (Employee employee : employees) {
            Map<Integer, String> lastWeekPattern = getLastWeekPattern(employee.getId(), previousMonth);
            List<Shift> employeeShifts = generateShiftsBasedOnPattern(employee.getId(), targetMonth, lastWeekPattern);
            newShifts.addAll(employeeShifts);
        }
        if (!newShifts.isEmpty()) {
            shiftDAO.saveBatch(newShifts);
        }
    }

    private boolean hasRealDataForMonth(YearMonth month) throws SQLException {
        Map<Integer, List<Shift>> shifts = shiftDAO.findShiftsForMonth(month);
        if (shifts.isEmpty()) {
            return false;
        }
        for (List<Shift> employeeShifts : shifts.values()) {
            for (Shift shift : employeeShifts) {
                if (shift.getCode() != null && !shift.getCode().equals("X")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void generateEmptySchedule(YearMonth month) throws SQLException {
        List<Employee> employees = employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .toList();
        List<Shift> emptyShifts = new ArrayList<>();
        for (Employee employee : employees) {
            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                emptyShifts.add(new Shift(employee.getId(), date, "X"));
            }
        }
        if (!emptyShifts.isEmpty()) {
            shiftDAO.saveBatch(emptyShifts);
        }
    }

    private Map<Integer, String> getLastWeekPattern(Integer employeeId, YearMonth month) throws SQLException {
        Map<Integer, String> lastWeek = new HashMap<>();
        int daysInMonth = month.lengthOfMonth();
        int startDay = Math.max(1, daysInMonth - 6);
        for (int day = startDay; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);
            Shift shift = shiftDAO.findForEmployeeOnDate(employeeId, date);
            String code = (shift != null && shift.getCode() != null) ? shift.getCode() : "X";
            lastWeek.put(day, code);
        }
        return lastWeek;
    }

    private List<Shift> generateShiftsBasedOnPattern(Integer employeeId, YearMonth month, Map<Integer, String> lastWeekPattern) {
        List<Shift> shifts = new ArrayList<>();
        int daysInMonth = month.lengthOfMonth();
        if (lastWeekPattern.isEmpty() || lastWeekPattern.values().stream().allMatch("X"::equals)) {
            for (int day = 1; day <= daysInMonth; day++) {
                shifts.add(new Shift(employeeId, month.atDay(day), "X"));
            }
            return shifts;
        }
        String detectedPattern = analyzePattern(lastWeekPattern);
        switch (detectedPattern) {
            case "1_3_1":
                for (int day = 1; day <= daysInMonth; day++) {
                    String code = (day % 4 == 1) ? "1" : "X";
                    shifts.add(new Shift(employeeId, month.atDay(day), code));
                }
                break;
            case "2_2_2":
                for (int day = 1; day <= daysInMonth; day++) {
                    int cycle = (day - 1) % 4;
                    String code = (cycle < 2) ? "2" : "X";
                    shifts.add(new Shift(employeeId, month.atDay(day), code));
                }
                break;
            case "1_2_mixed":
                for (int day = 1; day <= daysInMonth; day++) {
                    int cycle = (day - 1) % 4;
                    String code;
                    if (cycle == 0) code = "1";
                    else if (cycle == 1) code = "2";
                    else code = "X";
                    shifts.add(new Shift(employeeId, month.atDay(day), code));
                }
                break;
            case "weekday":
                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = month.atDay(day);
                    boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
                    String code = isWeekend ? "X" : "1";
                    shifts.add(new Shift(employeeId, date, code));
                }
                break;
            default:
                for (int day = 1; day <= daysInMonth; day++) {
                    shifts.add(new Shift(employeeId, month.atDay(day), "X"));
                }
        }
        return shifts;
    }

    private String analyzePattern(Map<Integer, String> lastWeekPattern) {
        if (lastWeekPattern.isEmpty()) return "empty";
        List<String> pattern = new ArrayList<>(lastWeekPattern.values());
        long count1 = pattern.stream().filter("1"::equals).count();
        long count2 = pattern.stream().filter(code -> "2".equals(code) || "12".equals(code)).count();
        long countX = pattern.stream().filter("X"::equals).count();
        if (count1 > 0 && count2 == 0 && countX > count1 * 2) {
            return "1_3_1";
        } else if (count2 > 0 && countX >= count2) {
            return "2_2_2";
        } else if (count1 > 0 && count2 > 0) {
            return "1_2_mixed";
        } else if (count1 > 0 && countX == 0) {
            return "weekday";
        }
        return "empty";
    }

    public void loadAndContinueSchedule(YearMonth targetMonth) throws SQLException {
        continueFromDatabase(targetMonth);
    }

    public void loadPredefinedScheduleForMonth(YearMonth month) {
        if (isPreinitializedMonth(month)) {
            DatabaseInitializer.loadThreeMonthsData();
        }
    }
}