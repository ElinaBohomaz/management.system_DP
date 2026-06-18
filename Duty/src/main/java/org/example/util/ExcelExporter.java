package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Employee;
import org.example.model.Shift;
import org.example.service.ScheduleService;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelExporter {

    public static void exportSchedule(ScheduleService scheduleService, YearMonth month, File outputFile) throws Exception {
        Map<Employee, List<Shift>> scheduleData = scheduleService.loadScheduleForMonth(month);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Графік змін");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle employeeStyle = createEmployeeStyle(workbook);
        CellStyle dayOffStyle = createDayOffStyle(workbook);
        CellStyle workingDayStyle = createWorkingDayStyle(workbook);
        CellStyle specialDayStyle = createSpecialDayStyle(workbook);
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Графік змін працівників за " + DateUtil.formatMonthYear(month));
        titleCell.setCellStyle(headerStyle);
        Row headerRow = sheet.createRow(2);
        String[] headers = {"ПІБ", "Підрозділ", "Посада"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        int daysInMonth = DateUtil.getDaysInMonthCount(month);
        for (int day = 1; day <= daysInMonth; day++) {
            Cell cell = headerRow.createCell(headers.length + day - 1);
            cell.setCellValue(String.valueOf(day));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(headers.length + day - 1, 1500);
        }
        int rowIndex = 3;
        for (Map.Entry<Employee, List<Shift>> entry : scheduleData.entrySet()) {
            Employee employee = entry.getKey();
            List<Shift> shifts = entry.getValue();
            Map<LocalDate, String> shiftMap = new HashMap<>();
            for (Shift shift : shifts) {
                shiftMap.put(shift.getDate(), shift.getCode());
            }
            Row dataRow = sheet.createRow(rowIndex);
            Cell nameCell = dataRow.createCell(0);
            nameCell.setCellValue(employee.getFullName());
            nameCell.setCellStyle(employeeStyle);
            Cell deptCell = dataRow.createCell(1);
            deptCell.setCellValue(employee.getDepartment());
            deptCell.setCellStyle(employeeStyle);
            Cell positionCell = dataRow.createCell(2);
            positionCell.setCellValue(employee.getPosition() != null ? employee.getPosition() : "");
            positionCell.setCellStyle(employeeStyle);
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = month.atDay(day);
                String shiftCode = shiftMap.getOrDefault(date, "X");
                Cell shiftCell = dataRow.createCell(headers.length + day - 1);
                shiftCell.setCellValue(shiftCode);
                CellStyle cellStyle = getCellStyleForShift(shiftCode, dayOffStyle, workingDayStyle, specialDayStyle);
                shiftCell.setCellStyle(cellStyle);
            }
            rowIndex++;
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    public static void exportEmployees(List<Employee> employees, File outputFile) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Працівники");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createEmployeeStyle(workbook);
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Список працівників станом на " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        titleCell.setCellStyle(headerStyle);
        Row headerRow = sheet.createRow(2);
        String[] headers = {"ID", "ПІБ", "Посада", "Підрозділ", "Освіта", "Телефон", "Дата народження", "Дата прийому", "Профком", "Діти", "Статус"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }
        int rowIndex = 3;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (Employee employee : employees) {
            Row dataRow = sheet.createRow(rowIndex);
            Cell idCell = dataRow.createCell(0);
            idCell.setCellValue(employee.getId());
            idCell.setCellStyle(dataStyle);
            Cell nameCell = dataRow.createCell(1);
            nameCell.setCellValue(employee.getFullName());
            nameCell.setCellStyle(dataStyle);
            Cell positionCell = dataRow.createCell(2);
            positionCell.setCellValue(employee.getPosition() != null ? employee.getPosition() : "");
            positionCell.setCellStyle(dataStyle);
            Cell deptCell = dataRow.createCell(3);
            deptCell.setCellValue(employee.getDepartment() != null ? employee.getDepartment() : "");
            deptCell.setCellStyle(dataStyle);
            Cell eduCell = dataRow.createCell(4);
            eduCell.setCellValue(employee.getEducation() != null ? employee.getEducation() : "");
            eduCell.setCellStyle(dataStyle);
            Cell phoneCell = dataRow.createCell(5);
            phoneCell.setCellValue(employee.getPhone() != null ? employee.getPhone() : "");
            phoneCell.setCellStyle(dataStyle);
            Cell birthCell = dataRow.createCell(6);
            if (employee.getBirthDate() != null) {
                birthCell.setCellValue(employee.getBirthDate().format(dateFormatter));
            }
            birthCell.setCellStyle(dataStyle);
            Cell hireCell = dataRow.createCell(7);
            if (employee.getHireDate() != null) {
                hireCell.setCellValue(employee.getHireDate().format(dateFormatter));
            }
            hireCell.setCellStyle(dataStyle);
            Cell profkomCell = dataRow.createCell(8);
            profkomCell.setCellValue(employee.getProfkom() != null ? employee.getProfkom() : "");
            profkomCell.setCellStyle(dataStyle);
            Cell childrenCell = dataRow.createCell(9);
            childrenCell.setCellValue(employee.getChildren() != null ? employee.getChildren() : "");
            childrenCell.setCellStyle(dataStyle);
            Cell statusCell = dataRow.createCell(10);
            statusCell.setCellValue(employee.getStatus() != null ? employee.getStatus() : "");
            statusCell.setCellStyle(dataStyle);
            rowIndex++;
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private static CellStyle getCellStyleForShift(String shiftCode, CellStyle dayOffStyle, CellStyle workingDayStyle, CellStyle specialDayStyle) {
        if (shiftCode == null || "X".equals(shiftCode)) {
            return dayOffStyle;
        } else if ("1".equals(shiftCode) || "12".equals(shiftCode)) {
            return workingDayStyle;
        } else {
            return specialDayStyle;
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createEmployeeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDayOffStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createWorkingDayStyle(Workbook workbook) {
        CellStyle style = createDayOffStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle createSpecialDayStyle(Workbook workbook) {
        CellStyle style = createDayOffStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public static void exportToCSV(ScheduleService scheduleService, YearMonth month, File outputFile) throws Exception {
        Map<Employee, List<Shift>> scheduleData = scheduleService.loadScheduleForMonth(month);
        try (java.io.PrintWriter writer = new java.io.PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("Графік змін за " + DateUtil.formatMonthYear(month));
            writer.println();
            writer.print("ПІБ;Підрозділ;Посада;");
            int daysInMonth = DateUtil.getDaysInMonthCount(month);
            for (int day = 1; day <= daysInMonth; day++) {
                writer.print("День " + day + ";");
            }
            writer.println();
            for (Map.Entry<Employee, List<Shift>> entry : scheduleData.entrySet()) {
                Employee employee = entry.getKey();
                List<Shift> shifts = entry.getValue();
                Map<LocalDate, String> shiftMap = new HashMap<>();
                for (Shift shift : shifts) {
                    shiftMap.put(shift.getDate(), shift.getCode());
                }
                writer.print(employee.getFullName() + ";");
                writer.print(employee.getDepartment() + ";");
                writer.print(employee.getPosition() != null ? employee.getPosition() + ";" : ";");
                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = month.atDay(day);
                    String shiftCode = shiftMap.getOrDefault(date, "X");
                    writer.print(shiftCode + ";");
                }
                writer.println();
            }
        }
    }
}