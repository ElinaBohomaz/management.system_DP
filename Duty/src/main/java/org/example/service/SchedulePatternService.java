package org.example.service;

import org.example.dao.ShiftDAO;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.dao.EmployeeDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class SchedulePatternService {

    private final EmployeeDAO employeeDAO;
    private final ShiftDAO shiftDAO;

    public SchedulePatternService() {
        this.employeeDAO = new EmployeeDAO();
        this.shiftDAO = new ShiftDAO();
    }

    public void continueScheduleFromPreviousMonth(YearMonth currentMonth) throws SQLException {
        YearMonth previousMonth = currentMonth.minusMonths(1);
        List<Employee> employees = employeeDAO.findAll().stream()
                .filter(Employee::isCurrentlyWorking)
                .toList();
        List<Shift> newShifts = new ArrayList<>();
        for (Employee employee : employees) {
            List<Shift> previousShifts = shiftDAO.findShiftsForEmployeeAndMonth(employee.getId(), previousMonth);
            if (previousShifts != null && !previousShifts.isEmpty()) {
                String pattern = detectPattern(previousShifts, previousMonth);
                List<Shift> continuedShifts = generateShiftsByPattern(employee.getId(), currentMonth, pattern, previousShifts);
                newShifts.addAll(continuedShifts);
            }
        }
        saveOnlyEmptyDays(newShifts, currentMonth);
    }

    private String detectPattern(List<Shift> shifts, YearMonth month) {
        shifts.sort(Comparator.comparing(Shift::getDate));
        Map<String, Integer> codeCount = new HashMap<>();
        List<String> sequence = new ArrayList<>();
        for (Shift shift : shifts) {
            String code = shift.getCode() == null ? "" : shift.getCode();
            if (!code.isEmpty() && !"X".equals(code)) {
                codeCount.put(code, codeCount.getOrDefault(code, 0) + 1);
            }
            sequence.add(code);
        }
        if (sequence.contains("2") || sequence.contains("12")) {
            return analyzeDoubleShiftPattern(sequence);
        } else if (sequence.contains("1")) {
            return analyzeSingleShiftPattern(sequence);
        } else if (sequence.contains("8.25")) {
            return "8_hours";
        }
        return "irregular";
    }

    private String analyzeDoubleShiftPattern(List<String> sequence) {
        int count1 = Collections.frequency(sequence, "1");
        int count2 = Collections.frequency(sequence, "2") + Collections.frequency(sequence, "12");
        if (count2 > count1 * 2) {
            return "2_2_2";
        } else {
            return "1_2_mixed";
        }
    }

    private String analyzeSingleShiftPattern(List<String> sequence) {
        int daysBetweenWork = 0;
        boolean counting = false;
        List<Integer> gaps = new ArrayList<>();
        for (int i = 0; i < sequence.size(); i++) {
            if ("1".equals(sequence.get(i))) {
                if (counting && daysBetweenWork > 0) {
                    gaps.add(daysBetweenWork);
                }
                counting = true;
                daysBetweenWork = 0;
            } else if (counting) {
                daysBetweenWork++;
            }
        }
        if (!gaps.isEmpty()) {
            int avgGap = (int) gaps.stream().mapToInt(Integer::intValue).average().orElse(3);
            if (avgGap >= 2 && avgGap <= 4) {
                return "1_" + avgGap + "_1";
            }
        }
        return "irregular";
    }

    private List<Shift> generateShiftsByPattern(Integer employeeId, YearMonth month, String pattern, List<Shift> previousShifts) {
        List<Shift> newShifts = new ArrayList<>();
        int daysInMonth = month.lengthOfMonth();
        List<String> lastDaysPattern = getLastDaysPattern(previousShifts, 7);
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);
            String shiftCode = generateShiftCodeForDay(day, pattern, lastDaysPattern, date.getDayOfWeek());
            newShifts.add(new Shift(employeeId, date, shiftCode));
        }
        return newShifts;
    }

    private List<String> getLastDaysPattern(List<Shift> shifts, int lastDays) {
        List<String> pattern = new ArrayList<>();
        shifts.sort(Comparator.comparing(Shift::getDate).reversed());
        int count = 0;
        for (Shift shift : shifts) {
            if (count >= lastDays) break;
            pattern.add(shift.getCode() == null ? "" : shift.getCode());
            count++;
        }
        Collections.reverse(pattern);
        return pattern;
    }

    private String generateShiftCodeForDay(int day, String pattern, List<String> lastPattern, java.time.DayOfWeek dayOfWeek) {
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return "X";
        }
        switch (pattern) {
            case "1_3_1":
                return (day % 4 == 1) ? "1" : "X";
            case "2_2_2":
                int cycle = (day - 1) % 4;
                return (cycle < 2) ? "2" : "X";
            case "1_2_mixed":
                if (!lastPattern.isEmpty()) {
                    int lastIndex = (day - 1) % lastPattern.size();
                    String code = lastPattern.get(lastIndex);
                    if ("1".equals(code) || "2".equals(code) || "12".equals(code)) {
                        return code;
                    }
                }
                return (day % 3 == 1) ? "1" : "X";
            case "8_hours":
                return "8.25";
            default:
                return "X";
        }
    }

    private void saveOnlyEmptyDays(List<Shift> newShifts, YearMonth month) throws SQLException {
        List<Shift> toSave = new ArrayList<>();
        for (Shift newShift : newShifts) {
            Shift existing = shiftDAO.findForEmployeeOnDate(newShift.getEmployeeId(), newShift.getDate());
            if (existing == null || "X".equals(existing.getCode())) {
                toSave.add(newShift);
            }
        }
        if (!toSave.isEmpty()) {
            shiftDAO.saveBatch(toSave);
        }
    }
}