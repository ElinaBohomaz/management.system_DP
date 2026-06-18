package org.example.dao;

import org.example.model.Shift;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftDAO {


    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:duty_schedule.db");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер SQLite не знайдено", e);
        }
    }

    public void save(Shift shift) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO shifts (employee_id, date, code)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, shift.getEmployeeId());
            pstmt.setDate(2, Date.valueOf(shift.getDate()));
            pstmt.setString(3, shift.getCode());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Помилка збереження зміни: " + e.getMessage());
            throw e;
        }
    }

    public void saveBatch(List<Shift> shifts) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO shifts (employee_id, date, code)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Shift shift : shifts) {
                pstmt.setInt(1, shift.getEmployeeId());
                pstmt.setDate(2, Date.valueOf(shift.getDate()));
                pstmt.setString(3, shift.getCode());
                pstmt.addBatch();
            }

            pstmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("Помилка пачкового збереження змін: " + e.getMessage());
            throw e;
        }
    }

    public Shift findForEmployeeOnDate(Integer employeeId, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM shifts WHERE employee_id = ? AND date = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToShift(rs);
            }

        } catch (SQLException e) {
            System.err.println("Помилка пошуку зміни: " + e.getMessage());
            throw e;
        }

        return null;
    }

    public Map<Integer, List<Shift>> findShiftsForMonth(YearMonth yearMonth) throws SQLException {
        Map<Integer, List<Shift>> shiftsMap = new HashMap<>();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        String sql = """
            SELECT s.*, e.full_name, e.department 
            FROM shifts s
            JOIN employees e ON s.employee_id = e.id
            WHERE date BETWEEN ? AND ?
            ORDER BY e.department, e.full_name, s.date
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Integer employeeId = rs.getInt("employee_id");
                Shift shift = mapResultSetToShift(rs);

                shiftsMap.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(shift);
            }

        } catch (SQLException e) {
            System.err.println("Помилка отримання змін за місяць: " + e.getMessage());
            throw e;
        }

        return shiftsMap;
    }

    public List<Shift> findShiftsForEmployeeAndMonth(Integer employeeId, YearMonth yearMonth)
            throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        String sql = """
            SELECT * FROM shifts 
            WHERE employee_id = ? AND date BETWEEN ? AND ? 
            ORDER BY date
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                shifts.add(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("Помилка отримання змін працівника: " + e.getMessage());
            throw e;
        }

        return shifts;
    }

    public void deleteForEmployeeAndMonth(Integer employeeId, YearMonth yearMonth)
            throws SQLException {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        String sql = "DELETE FROM shifts WHERE employee_id = ? AND date BETWEEN ? AND ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Помилка видалення змін: " + e.getMessage());
            throw e;
        }
    }

    public List<Integer> findEmployeesWorkingOnDate(LocalDate date) throws SQLException {
        List<Integer> employeeIds = new ArrayList<>();
        String sql = """
            SELECT DISTINCT employee_id 
            FROM shifts 
            WHERE date = ? AND (code = '1' OR code = '12' OR code = '11' OR code = '8.25')
            """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                employeeIds.add(rs.getInt("employee_id"));
            }

        } catch (SQLException e) {
            System.err.println("Помилка пошуку працюючих працівників: " + e.getMessage());
            throw e;
        }

        return employeeIds;
    }


    private Shift mapResultSetToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();

        shift.setId(rs.getInt("id"));
        shift.setEmployeeId(rs.getInt("employee_id"));
        shift.setDate(rs.getDate("date").toLocalDate());
        shift.setCode(rs.getString("code"));

        return shift;
    }
}