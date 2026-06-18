package org.example.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class Validator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+380|0)\\d{9}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-ЯІЇЄҐ][а-яіїєґ]+\\s[А-ЯІЇЄҐ]\\.\\s[А-ЯІЇЄҐ]\\.$");

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        return NAME_PATTERN.matcher(fullName.trim()).matches();
    }

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

    public static boolean isValidPastDate(LocalDate date) {
        if (date == null) {
            return true;
        }
        return !date.isAfter(LocalDate.now());
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}