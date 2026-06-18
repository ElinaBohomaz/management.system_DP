package org.example.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateUtil {

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("LLLL yyyy");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("d");
    private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static List<LocalDate> getDaysInMonth(YearMonth yearMonth) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate date = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        while (!date.isAfter(endDate)) {
            days.add(date);
            date = date.plusDays(1);
        }
        return days;
    }

    public static String formatMonthYear(YearMonth yearMonth) {
        String formatted = yearMonth.format(MONTH_YEAR_FORMATTER);
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    public static String formatDateForSql(LocalDate date) {
        return date.format(SQL_DATE_FORMATTER);
    }

    public static String getDayNumber(LocalDate date) {
        return date.format(DAY_FORMATTER);
    }

    public static int getDaysInMonthCount(YearMonth yearMonth) {
        return yearMonth.lengthOfMonth();
    }

    public static YearMonth getPreviousMonth(YearMonth currentMonth) {
        return currentMonth.minusMonths(1);
    }

    public static YearMonth getNextMonth(YearMonth currentMonth) {
        return currentMonth.plusMonths(1);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    public static YearMonth parseYearMonth(String monthYear) {
        try {
            if (monthYear.contains("-")) {
                return YearMonth.parse(monthYear);
            } else {
                String[] parts = monthYear.split(" ");
                if (parts.length == 2) {
                    int month = Integer.parseInt(parts[0]);
                    int year = Integer.parseInt(parts[1]);
                    return YearMonth.of(year, month);
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу дати: " + e.getMessage());
        }
        return YearMonth.now();
    }

    public static YearMonth getCurrentMonth() {
        return YearMonth.now();
    }

    public static List<Integer> generateYears() {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 2; i <= currentYear + 2; i++) {
            years.add(i);
        }
        return years;
    }

    public static String[] getUkrainianMonths() {
        return new String[]{"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень", "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
    }
}