package org.servantscode.ministry.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.ministry.MinistryRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class RoleDB extends DBAccess {

    public int getCount(int ministryId, String search) {
        String sql = format("SELECT count(1) from ministry_roles WHERE ministry_id=?%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, ministryId);

            try(ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not get role roles with names containing " + search, e);
        }
    }

    public List<MinistryRole> getRoles(int ministryId, String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM ministry_roles WHERE ministry_id=?%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, ministryId);
            stmt.setInt(2, count);
            stmt.setInt(3, start);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get roles with names containing " + search, e);
        }
    }

    public MinistryRole getRole(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ministry_roles WHERE id=?")
        ) {
            stmt.setInt(1, id);

            List<MinistryRole> results = processResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get role by id: " + id, e);
        }
    }

    public void create(MinistryRole role) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministry_roles(name, ministry_id, contact, leader) values (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, role.getName());
            stmt.setInt(2, role.getMinistryId());
            stmt.setBoolean(3, role.isContact());
            stmt.setBoolean(4, role.isLeader());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create role: " + role.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    role.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create role: " + role.getName(), e);
        }
    }

    public void update(MinistryRole role) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE ministry_roles SET name=?, ministry_id=?, contact=?, leader=? WHERE id=?")
        ){

            stmt.setString(1, role.getName());
            stmt.setInt(2, role.getMinistryId());
            stmt.setBoolean(3, role.isContact());
            stmt.setBoolean(4, role.isLeader());
            stmt.setInt(5, role.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update role: " + role.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update role: " + role.getName(), e);
        }
    }

    public boolean delete(MinistryRole role) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministry_roles WHERE id=?")
        ){

            stmt.setInt(1, role.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete role: " + role.getName(), e);
        }
    }

    // ----- Private ------
    private List<MinistryRole> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<MinistryRole> roles = new ArrayList<>();
            while(rs.next()) {
                MinistryRole role = new MinistryRole();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
                role.setMinistryId(rs.getInt("ministry_id"));
                role.setContact(rs.getBoolean("contact"));
                role.setLeader(rs.getBoolean("leader"));
                roles.add(role);
            }
            return roles;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search)? format(" AND name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}
