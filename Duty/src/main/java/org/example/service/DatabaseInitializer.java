package org.example.service;

import org.example.model.Employee;
import org.example.model.Shift;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseInitializer {

    private static final String DB_URL = "jdbc:sqlite:duty_schedule.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                createTables(conn);
                updateDatabaseSchema(conn);
                checkAndInsertRealData(conn);
            }
        } catch (SQLException e) {
            System.err.println("Помилка ініціалізації БД: " + e.getMessage());
        }
    }

    private static void checkAndInsertRealData(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM employees");

        if (rs.next() && rs.getInt("count") == 0) {
            insertRealEmployees(conn);
            initializeThreeMonthsData(conn);
            insertEmptyShiftsForOtherMonths(conn);
        } else {
            boolean hasDataForThreeMonths = checkThreeMonthsData(conn);
            if (!hasDataForThreeMonths) {
                initializeThreeMonthsData(conn);
            }
        }

        rs.close();
        stmt.close();
    }

    private static boolean checkThreeMonthsData(Connection conn) throws SQLException {
        YearMonth[] months = {
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2)
        };

        for (YearMonth month : months) {
            String sql = "SELECT COUNT(*) as count FROM shifts WHERE date BETWEEN ? AND ? AND code != 'X'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(month.atDay(1)));
            pstmt.setDate(2, Date.valueOf(month.atEndOfMonth()));

            ResultSet rs = pstmt.executeQuery();

            if (rs.next() && rs.getInt("count") == 0) {
                rs.close();
                pstmt.close();
                return false;
            }

            rs.close();
            pstmt.close();
        }

        return true;
    }

    public static void initializeThreeMonthsData(Connection conn) throws SQLException {
        System.out.println("Ініціалізація графіків на грудень 2025, січень 2026, лютий 2026...");

        List<Employee> employees = getAllEmployees(conn);
        List<Shift> allShifts = new ArrayList<>();

        YearMonth[] months = {
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2)
        };

        int employeeIndex = 0;

        for (Employee employee : employees) {
            if (!employee.isCurrentlyWorking()) {
                continue;
            }

            boolean weekdaySchedule = employeeIndex % 3 == 0;

            for (YearMonth month : months) {
                for (int day = 1; day <= month.lengthOfMonth(); day++) {
                    LocalDate date = month.atDay(day);
                    String code;

                    if (weekdaySchedule) {
                        DayOfWeek dayOfWeek = date.getDayOfWeek();

                        if (dayOfWeek == DayOfWeek.MONDAY
                                || dayOfWeek == DayOfWeek.TUESDAY
                                || dayOfWeek == DayOfWeek.WEDNESDAY
                                || dayOfWeek == DayOfWeek.THURSDAY) {
                            code = "8.25";
                        } else if (dayOfWeek == DayOfWeek.FRIDAY) {
                            code = "7.00";
                        } else {
                            code = "X";
                        }
                    } else {
                        int cycle = (day - 1) % 4;
                        code = cycle == 0 ? "1" : "X";
                    }

                    allShifts.add(new Shift(employee.getId(), date, code));
                }
            }

            employeeIndex++;
        }

        String insertShiftSQL = "INSERT OR REPLACE INTO shifts (employee_id, date, code) VALUES (?, ?, ?)";
        PreparedStatement shiftStmt = conn.prepareStatement(insertShiftSQL);

        int batchSize = 0;

        for (Shift shift : allShifts) {
            shiftStmt.setInt(1, shift.getEmployeeId());
            shiftStmt.setDate(2, Date.valueOf(shift.getDate()));
            shiftStmt.setString(3, shift.getCode());
            shiftStmt.addBatch();

            batchSize++;

            if (batchSize % 100 == 0) {
                shiftStmt.executeBatch();
            }
        }

        shiftStmt.executeBatch();
        shiftStmt.close();

        System.out.println("Вставлено " + allShifts.size() + " змін для трьох місяців");
    }

    public static void loadThreeMonthsData() {
        System.out.println("Завантаження графіків на грудень 2025, січень 2026, лютий 2026...");

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            initializeThreeMonthsData(conn);
        } catch (SQLException e) {
            System.err.println("Помилка завантаження графіків: " + e.getMessage());
        }
    }

    private static void insertRealEmployees(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            List<Employee> employees = createRealEmployeeList();

            String insertEmployeeSQL = """
                INSERT INTO employees (full_name, position, department, education, phone, 
                     birth_date, hire_date, status, shift_type, days_off_after, days_off_before, 
                     pattern_type, profkom, children, data, last_work_code, last_x_count, last_work_day)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement employeeStmt = conn.prepareStatement(insertEmployeeSQL, Statement.RETURN_GENERATED_KEYS);

            for (Employee employee : employees) {
                employeeStmt.setString(1, employee.getFullName());
                employeeStmt.setString(2, employee.getPosition());
                employeeStmt.setString(3, employee.getDepartment());
                employeeStmt.setString(4, employee.getEducation());
                employeeStmt.setString(5, employee.getPhone());

                if (employee.getBirthDate() != null) {
                    employeeStmt.setDate(6, Date.valueOf(employee.getBirthDate()));
                } else {
                    employeeStmt.setNull(6, Types.DATE);
                }

                if (employee.getHireDate() != null) {
                    employeeStmt.setDate(7, Date.valueOf(employee.getHireDate()));
                } else {
                    employeeStmt.setNull(7, Types.DATE);
                }

                employeeStmt.setString(8, employee.getStatus());
                employeeStmt.setString(9, employee.getShiftType());
                employeeStmt.setInt(10, employee.getDaysOffAfter());
                employeeStmt.setInt(11, employee.getDaysOffBefore());
                employeeStmt.setString(12, employee.getPatternType());
                employeeStmt.setString(13, employee.getProfkom());
                employeeStmt.setString(14, employee.getChildren());
                employeeStmt.setString(15, employee.getData());
                employeeStmt.setString(16, employee.getLastWorkCode());
                employeeStmt.setInt(17, employee.getLastXCount());
                employeeStmt.setInt(18, employee.getLastWorkDay());
                employeeStmt.executeUpdate();

                ResultSet generatedKeys = employeeStmt.getGeneratedKeys();

                if (generatedKeys.next()) {
                    employee.setId(generatedKeys.getInt(1));
                }

                generatedKeys.close();
            }

            employeeStmt.close();
            conn.commit();

            System.out.println("Вставлено " + employees.size() + " працівників");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static List<Employee> createRealEmployeeList() {
        List<Employee> employees = new ArrayList<>();

        employees.add(createEmployee("Верещака Т.Д.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Опішнян Г.М.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Шкурко С.В.", "МНУ 2 р.", "Великорогізнянська"));
        employees.add(createEmployee("Поварніцина Т.В.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Курочка С.М.", "МНУ 4 р.", "Великорогізнянська"));
        employees.add(createEmployee("Мисяк Ю.О.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee("Горбатко Я.В.", "МНУ 2р.", "ГКНС"));
        employees.add(createEmployee("Тесленко П.В.", "МНУ 4 р.", "ГКНС"));
        employees.add(createEmployee("Нестеренко Ю.А.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee("Шаповалова Л.А.", "МНУ 2 р.", "ГКНС"));
        employees.add(createEmployee("Зірка Л.В.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Мороз Т.І.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Юсковець Т.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Хорошун Д.М.", "МНУ 2 р.", "Пром.район"));
        employees.add(createEmployee("Міхєєва Л.М.", "МНУ 2 р.", "Пром.район"));

        return employees;
    }

    private static Employee createEmployee(String fullName, String position, String department) {
        Employee employee = new Employee();

        employee.setFullName(fullName);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setStatus("працює");
        employee.setShiftType("1");
        employee.setEducation("");
        employee.setPhone("");
        employee.setDaysOffAfter(1);
        employee.setDaysOffBefore(0);
        employee.setPatternType("1_3_1");
        employee.setProfkom("");
        employee.setChildren("");
        employee.setData("");
        employee.setLastWorkCode("");
        employee.setLastXCount(0);
        employee.setLastWorkDay(0);

        return employee;
    }

    private static void insertEmptyShiftsForOtherMonths(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            List<Employee> employees = getAllEmployees(conn);

            YearMonth[] months = {
                    YearMonth.of(2025, 1), YearMonth.of(2025, 2), YearMonth.of(2025, 3),
                    YearMonth.of(2025, 4), YearMonth.of(2025, 5), YearMonth.of(2025, 6),
                    YearMonth.of(2025, 7), YearMonth.of(2025, 8), YearMonth.of(2025, 9),
                    YearMonth.of(2025, 10), YearMonth.of(2025, 11),
                    YearMonth.of(2026, 3), YearMonth.of(2026, 4), YearMonth.of(2026, 5),
                    YearMonth.of(2026, 6), YearMonth.of(2026, 7), YearMonth.of(2026, 8),
                    YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11),
                    YearMonth.of(2026, 12)
            };

            String insertShiftSQL = "INSERT OR IGNORE INTO shifts (employee_id, date, code) VALUES (?, ?, ?)";
            PreparedStatement shiftStmt = conn.prepareStatement(insertShiftSQL);

            int totalShifts = 0;

            for (YearMonth month : months) {
                for (Employee employee : employees) {
                    if (employee.isCurrentlyWorking()) {
                        for (int day = 1; day <= month.lengthOfMonth(); day++) {
                            LocalDate date = month.atDay(day);

                            shiftStmt.setInt(1, employee.getId());
                            shiftStmt.setDate(2, Date.valueOf(date));
                            shiftStmt.setString(3, "X");
                            shiftStmt.addBatch();

                            totalShifts++;

                            if (totalShifts % 500 == 0) {
                                shiftStmt.executeBatch();
                            }
                        }
                    }
                }
            }

            shiftStmt.executeBatch();
            shiftStmt.close();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static List<Employee> getAllEmployees(Connection conn) throws SQLException {
        List<Employee> employees = new ArrayList<>();

        String sql = "SELECT * FROM employees";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Employee employee = new Employee();

                employee.setId(rs.getInt("id"));
                employee.setFullName(rs.getString("full_name"));
                employee.setPosition(rs.getString("position"));
                employee.setDepartment(rs.getString("department"));
                employee.setEducation(rs.getString("education"));
                employee.setPhone(rs.getString("phone"));

                Date birthDate = rs.getDate("birth_date");
                if (birthDate != null) {
                    employee.setBirthDate(birthDate.toLocalDate());
                }

                Date hireDate = rs.getDate("hire_date");
                if (hireDate != null) {
                    employee.setHireDate(hireDate.toLocalDate());
                }

                employee.setStatus(rs.getString("status"));
                employee.setShiftType(rs.getString("shift_type"));
                employee.setDaysOffAfter(rs.getInt("days_off_after"));
                employee.setDaysOffBefore(rs.getInt("days_off_before"));
                employee.setPatternType(rs.getString("pattern_type"));
                employee.setProfkom(rs.getString("profkom"));
                employee.setChildren(rs.getString("children"));
                employee.setData(rs.getString("data"));
                employee.setLastWorkCode(rs.getString("last_work_code"));
                employee.setLastXCount(rs.getInt("last_x_count"));
                employee.setLastWorkDay(rs.getInt("last_work_day"));

                employees.add(employee);
            }
        }

        return employees;
    }

    public static Map<String, Integer> getEmployeeIdMap(Connection conn) throws SQLException {
        Map<String, Integer> employeeIdMap = new HashMap<>();

        String sql = "SELECT id, full_name FROM employees";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                employeeIdMap.put(rs.getString("full_name"), rs.getInt("id"));
            }
        }

        return employeeIdMap;
    }

    private static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        String createEmployeesTable = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                position TEXT,
                department TEXT,
                education TEXT,
                phone TEXT,
                birth_date DATE,
                hire_date DATE,
                status TEXT DEFAULT 'працює',
                shift_type TEXT DEFAULT '1',
                days_off_after INTEGER DEFAULT 1,
                days_off_before INTEGER DEFAULT 0,
                pattern_type TEXT DEFAULT '1_3_1',
                profkom TEXT DEFAULT '',
                children TEXT DEFAULT '',
                data TEXT DEFAULT '',
                last_work_code TEXT DEFAULT '',
                last_x_count INTEGER DEFAULT 0,
                last_work_day INTEGER DEFAULT 0
            )
        """;

        String createShiftsTable = """
            CREATE TABLE IF NOT EXISTS shifts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id INTEGER NOT NULL,
                date DATE NOT NULL,
                code TEXT DEFAULT 'X',
                notes TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
                UNIQUE (employee_id, date)
            )
        """;

        stmt.execute(createEmployeesTable);
        stmt.execute(createShiftsTable);
        stmt.close();
    }

    private static void updateDatabaseSchema(Connection conn) {
        try {
            String[] newColumns = {
                    "profkom",
                    "children",
                    "data",
                    "last_work_code",
                    "last_x_count",
                    "last_work_day"
            };

            for (String column : newColumns) {
                if (!isColumnExists(conn, "employees", column)) {
                    addColumnToEmployees(conn, column);
                }
            }

            createIndexes(conn);
        } catch (SQLException e) {
            System.err.println("Помилка оновлення схеми БД: " + e.getMessage());
        }
    }

    private static void createIndexes(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shifts_date ON shifts(date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shifts_employee_date ON shifts(employee_id, date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department)");
        } finally {
            stmt.close();
        }
    }

    private static boolean isColumnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void addColumnToEmployees(Connection conn, String column) throws SQLException {
        String sqlType;

        switch (column) {
            case "profkom":
            case "children":
            case "data":
            case "last_work_code":
                sqlType = "TEXT DEFAULT ''";
                break;
            case "last_x_count":
            case "last_work_day":
                sqlType = "INTEGER DEFAULT 0";
                break;
            default:
                return;
        }

        String sql = "ALTER TABLE employees ADD COLUMN " + column + " " + sqlType;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static void resetDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("DELETE FROM shifts");
            stmt.execute("DELETE FROM employees");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='employees'");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='shifts'");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Помилка скидання БД: " + e.getMessage());
        }
    }

    public static boolean hasShiftsForMonth(YearMonth month) {
        String sql = "SELECT COUNT(*) as count FROM shifts WHERE date BETWEEN ? AND ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(month.atDay(1)));
            pstmt.setDate(2, Date.valueOf(month.atEndOfMonth()));

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Помилка перевірки наявності графіків: " + e.getMessage());
        }

        return false;
    }

    public static boolean hasRealDataForMonth(YearMonth month) {
        String sql = "SELECT COUNT(*) as count FROM shifts WHERE date BETWEEN ? AND ? AND code != 'X'";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(month.atDay(1)));
            pstmt.setDate(2, Date.valueOf(month.atEndOfMonth()));

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Помилка перевірки наявності реальних даних: " + e.getMessage());
        }

        return false;
    }

    public static int getEmployeeCount() {
        String sql = "SELECT COUNT(*) as count FROM employees";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання кількості працівників: " + e.getMessage());
        }

        return 0;
    }
}