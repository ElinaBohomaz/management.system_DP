package org.example.service;

import org.example.model.Employee;
import org.example.model.Shift;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.util.*;

public class PatternService {

    public String detectPattern(List<Shift> shifts, YearMonth month) {
        if (shifts == null || shifts.isEmpty()) {
            return "1_3_1";
        }

        shifts.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        List<String> sequence = new ArrayList<>();
        for (Shift shift : shifts) {
            String code = shift.getCode();
            if ("1".equals(code) || "2".equals(code) || "12".equals(code)) {
                sequence.add(code);
            } else if ("X".equals(code)) {
                sequence.add("X");
            } else if ("0".equals(code)) {
                sequence.add("0");
            } else if ("В".equals(code)) {
                sequence.add("В");
            } else if ("ТН".equals(code)) {
                sequence.add("ТН");
            } else if ("8.25".equals(code)) {
                sequence.add("8.25");
            } else if ("7.0".equals(code)) {
                sequence.add("7.0");
            } else {
                sequence.add(code);
            }
        }

        if (isPattern1_3_1(sequence)) {
            return "1_3_1";
        } else if (isPattern2_2(sequence)) {
            return "2_2";
        } else if (isPattern12_2_12(sequence)) {
            return "12_2_12";
        } else if (isPatternWeekday(sequence, month)) {
            return "weekday";
        } else if (isPattern8_25(sequence, month)) {
            return "8_25";
        }

        return "1_3_1";
    }

    private boolean isPattern1_3_1(List<String> sequence) {
        for (int i = 0; i < sequence.size() - 4; i++) {
            if (isWorkCode(sequence.get(i)) &&
                    "X".equals(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "X".equals(sequence.get(i+3)) &&
                    isWorkCode(sequence.get(i+4))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPattern2_2(List<String> sequence) {
        for (int i = 0; i < sequence.size() - 3; i++) {
            if (isWorkCode(sequence.get(i)) &&
                    isWorkCode(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "X".equals(sequence.get(i+3))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPattern12_2_12(List<String> sequence) {
        for (int i = 0; i < sequence.size() - 3; i++) {
            if ("12".equals(sequence.get(i)) &&
                    "X".equals(sequence.get(i+1)) &&
                    "X".equals(sequence.get(i+2)) &&
                    "12".equals(sequence.get(i+3))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPatternWeekday(List<String> sequence, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        int totalWorkDays = 0;
        int totalWeekdays = 0;
        LocalDate currentDate = startDate;
        int index = 0;

        while (!currentDate.isAfter(endDate) && index < sequence.size()) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekday = dayOfWeek.getValue() <= 5;
            if (isWeekday) {
                totalWeekdays++;
                if (isWorkCode(sequence.get(index))) {
                    totalWorkDays++;
                }
            }
            currentDate = currentDate.plusDays(1);
            index++;
        }
        return totalWeekdays > 0 && totalWorkDays >= totalWeekdays * 0.8;
    }

    private boolean isPattern8_25(List<String> sequence, YearMonth month) {
        for (String code : sequence) {
            if ("8.25".equals(code) || "7.0".equals(code)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorkCode(String code) {
        return "1".equals(code) || "2".equals(code) || "12".equals(code) ||
                "8.25".equals(code) || "7.0".equals(code);
    }

    public String getPatternDisplayName(String patternType) {
        switch (patternType) {
            case "1_3_1": return "1 день роботи / 3 дні відпочинку";
            case "2_2": return "2 дні роботи / 2 дні відпочинку";
            case "12_2_12": return "12 год роботи / 2 дні вдома / 12 год ніч";
            case "weekday": return "Тільки в будні дні";
            case "8_25": return "Тільки в будні по 8.25 год";
            default: return patternType;
        }
    }

    public List<Integer> predictNextWorkDays(Employee employee, List<Shift> shifts, YearMonth month, int daysToPredict) {
        List<Integer> predictedDays = new ArrayList<>();
        if (shifts == null || shifts.isEmpty()) {
            return predictedDays;
        }

        String pattern = detectPattern(shifts, month);
        int daysInMonth = month.lengthOfMonth();
        Set<Integer> existingWorkDays = new HashSet<>();

        for (Shift shift : shifts) {
            if (isWorkCode(shift.getCode())) {
                existingWorkDays.add(shift.getDate().getDayOfMonth());
            }
        }

        for (int day = 1; day <= daysInMonth && predictedDays.size() < daysToPredict; day++) {
            if (existingWorkDays.contains(day)) continue;

            boolean shouldWork = false;
            switch (pattern) {
                case "1_3_1":
                    shouldWork = (day % 4 == 1);
                    break;
                case "2_2":
                    shouldWork = (day % 4 <= 1);
                    break;
                case "weekday":
                    LocalDate date = month.atDay(day);
                    shouldWork = date.getDayOfWeek().getValue() <= 5;
                    break;
                default:
                    shouldWork = false;
            }

            if (shouldWork) {
                predictedDays.add(day);
            }
        }

        return predictedDays;
    }
}