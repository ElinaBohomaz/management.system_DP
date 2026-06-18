package org.example.data;

import org.example.model.Employee;
import org.example.model.Shift;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class InitialData {

    public static List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        int id = 1;

        employees.add(createEmployee(id++, "Мисяк Ю.О.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Горбатко Я.В.", "МНУ 2р.", "ГКНС"));
        employees.add(createEmployee(id++, "Тесленко П.В.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Нестеренко Ю.А.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Лісівець С.Л.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Шаповалова Л.А.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Науменко Г.Ф.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Скрипник В.О.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee(id++, "Педашенко А.І.", "МНУ 4 р.", "ГКНС"));

        employees.add(createEmployee(id++, "Верещака Т.Д.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Опішнян Г.М.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Лісівець О.А.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Шкурко С.В.", "МНУ 2 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Поварніцина Т.В.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Голуб О.М.", "МНУ 2 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Селюк В.В.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Курочка С.М.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Горіздра Т.В.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee(id++, "Мокринська Л.І.", "Опер.на реш. 2р.", "Великорогізнянська"));

        employees.add(createEmployee(id++, "Зірка Л.В.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Щущенко В.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Мокринська Л.І.", "Опер.на реш. 2р.", "Пром.район"));
        employees.add(createEmployee(id++, "Мороз Т.І.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Юсковець Т.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Хорошун Д.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Міхєєва Л.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee(id++, "Мотрій Н.М.", "МНУ 2 р.", "Пром.район"));

        return employees;
    }

    private static Employee createEmployee(int id, String fullName, String position, String department) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(fullName);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setStatus("працює");
        employee.setShiftType("1");
        employee.setEducation("");
        employee.setPhone("");
        employee.setDaysOffAfter(1);
        employee.setDaysOffBefore(0);
        return employee;
    }

    public static List<Shift> getShiftsForJanuary2026(List<Employee> employees) {
        List<Shift> shifts = new ArrayList<>();
        YearMonth january2026 = YearMonth.of(2026, 1);

        for (Employee employee : employees) {

            for (int day = 1; day <= 31; day++) {
                LocalDate date = january2026.atDay(day);
                shifts.add(new Shift(employee.getId(), date, "X"));
            }
        }

        return shifts;
    }
}