package com.example.coffeedms;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed repository for CoffeeBean, using MySQL.
 * Attempts both modern and legacy driver class names.
 */
public class DBBeanRepository {
    private final Connection conn;

    /**
     * @param url  JDBC URL (e.g. "jdbc:mysql://localhost:3306/coffee_dms")
     * @param user MySQL username
     * @param pass MySQL password
     * @throws SQLException if driver missing or connection fails
     */
    public DBBeanRepository(String url, String user, String pass) throws SQLException {
        // force driver registration
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e2) {
                throw new SQLException("MySQL JDBC driver not found on classpath", e2);
            }
        }
        // ensure serverTimezone param
        String fullUrl = url.contains("?")
                ? url + "&serverTimezone=UTC"
                : url + "?serverTimezone=UTC";
        conn = DriverManager.getConnection(fullUrl, user, pass);
    }

    public List<CoffeeBean> findAll() throws SQLException {
        String sql = "SELECT * FROM bean_lots ORDER BY bean_id";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<CoffeeBean> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        }
    }

    public CoffeeBean findByID(String id) throws SQLException {
        String sql = "SELECT * FROM bean_lots WHERE bean_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean add(CoffeeBean b) throws SQLException {
        String sql = "INSERT INTO bean_lots VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBeanID());
            ps.setString(2, b.getOriginCountry());
            ps.setString(3, b.getFarmName());
            ps.setString(4, b.getRoastLevel().name());
            ps.setDate(5, Date.valueOf(b.getRoastDate()));
            ps.setDouble(6, b.getQuantityKg());
            ps.setBigDecimal(7, b.getCostPerKg());
            ps.setString(8, b.getFlavorNotes());
            ps.setDouble(9, b.getCaffeineContentMgPerGram());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean update(CoffeeBean b) throws SQLException {
        String sql = "UPDATE bean_lots SET origin=?, farm=?, roast_level=?, roast_date=?,"
                + " quantity_kg=?, cost_per_kg=?, notes=?, caffeine_mg_per_g=?"
                + " WHERE bean_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getOriginCountry());
            ps.setString(2, b.getFarmName());
            ps.setString(3, b.getRoastLevel().name());
            ps.setDate(4, Date.valueOf(b.getRoastDate()));
            ps.setDouble(5, b.getQuantityKg());
            ps.setBigDecimal(6, b.getCostPerKg());
            ps.setString(7, b.getFlavorNotes());
            ps.setDouble(8, b.getCaffeineContentMgPerGram());
            ps.setString(9, b.getBeanID());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean removeByID(String id) throws SQLException {
        String sql = "DELETE FROM bean_lots WHERE bean_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public BigDecimal calculateTotalInventoryValue() throws SQLException {
        String sql = "SELECT SUM(quantity_kg * cost_per_kg) AS total FROM bean_lots";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
        }
    }

    private CoffeeBean mapRow(ResultSet rs) throws SQLException {
        return new CoffeeBean(
                rs.getString("bean_id"),
                rs.getString("origin"),
                rs.getString("farm"),
                RoastLevel.valueOf(rs.getString("roast_level")),
                rs.getDate("roast_date").toLocalDate(),
                rs.getDouble("quantity_kg"),
                rs.getBigDecimal("cost_per_kg"),
                rs.getString("notes"),
                rs.getDouble("caffeine_mg_per_g")
        );
    }
}
