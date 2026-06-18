package org.example.model;

public class ShiftHelper {


    public static boolean isWorkingShift(Shift shift) {
        if (shift == null || shift.getCode() == null) return false;
        String code = shift.getCode();
        return isWorkingCode(code);
    }

    public static boolean isSpecialShift(Shift shift) {
        if (shift == null || shift.getCode() == null) return false;
        String code = shift.getCode();
        return isSpecialStatus(code);
    }

    public static boolean isWorkingCode(String code) {
        if (code == null) return false;

        code = normalizeShiftCode(code);

        return "1".equals(code) || "12".equals(code) || "2".equals(code) ||
                "8".equals(code) || "8.00".equals(code) || "8.25".equals(code) ||
                "7.00".equals(code);
    }


    public static boolean isSpecialStatus(String code) {
        if (code == null) return false;

        code = normalizeShiftCode(code);
        return "Л".equals(code) || "В".equals(code) || "К".equals(code) ||
                "ТН".equals(code) || "0".equals(code);
    }

    public static boolean isValidShiftCode(String code) {
        if (code == null || code.isEmpty()) return true;

        code = code.trim();

        if (isWorkingCode(code)) return true;

        if (isSpecialStatus(code)) return true;

        if ("X".equalsIgnoreCase(code)) return true;

        if ("0".equals(code)) return true;

        if ("8".equals(code)) return true;

        return false;
    }


    public static String normalizeShiftCode(String code) {
        if (code == null || code.isEmpty()) return code;

        code = code.trim().toUpperCase();


        if (code.matches("^[78],\\d{2}$")) {
            code = code.replace(",", ".");
        }

        return code;
    }


    public static String getDisplayName(String code) {
        if (code == null) return "Невизначено";

        code = normalizeShiftCode(code);

        switch (code) {
            case "1": return "Денна зміна";
            case "2":
            case "12": return "Подвійна зміна";
            case "X": return "Вихідний";
            case "0": return "Відгул";
            case "8": return "Перенесення";
            case "Л": return "Лікарняний";
            case "В": return "Відпустка";
            case "К": return "Відрядження";
            case "ТН": return "Тимчасово непрацездатний";
            default: {
                if (code.matches("^\\d+\\.\\d{2}$")) {
                    return code + " годин";
                }
                return code;
            }
        }
    }

    public static String getPatternTypeFromCode(String code) {
        if (code == null) return "1_3_1";

        code = normalizeShiftCode(code);

        if ("2".equals(code) || "12".equals(code)) {
            return "2_2";
        } else if ("1".equals(code)) {
            return "1_3_1";
        } else if ("8".equals(code) || "8.00".equals(code) || "8.25".equals(code)) {
            return "weekday";
        } else {
            return "1_3_1";
        }
    }
}