package org.example.model;

public class ShiftStatus {

    public static boolean isWorkingStatus(String code) {
        return ShiftHelper.isWorkingCode(code);
    }

    public static boolean isSpecialStatus(String code) {
        return ShiftHelper.isSpecialStatus(code);
    }

    public static boolean isValidStatus(String code) {
        return ShiftHelper.isValidShiftCode(code);
    }

    public static ShiftStatus fromCode(String code) {
        if (code == null) return null;

        String normalizedCode = ShiftHelper.normalizeShiftCode(code);
        String displayName = ShiftHelper.getDisplayName(normalizedCode);

        if (ShiftHelper.isWorkingCode(normalizedCode)) {
            return new ShiftStatus(displayName, "working");
        } else if (ShiftHelper.isSpecialStatus(normalizedCode)) {
            return new ShiftStatus(displayName, "special");
        } else if ("X".equals(normalizedCode)) {
            return new ShiftStatus(displayName, "off");
        } else {
            return new ShiftStatus(displayName, "unknown");
        }
    }

    private String name;
    private String type;

    private ShiftStatus(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return name;
    }
}