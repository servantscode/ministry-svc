package org.servantscode.ministry.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.ministry.MinistryRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class RoleDB extends DBAccess {

    private SearchParser<MinistryRole> searchParser;

    public RoleDB() {
        searchParser = new SearchParser<>(MinistryRole.class);
    }

    public int getCount(int ministryId, String search) {
        QueryBuilder query = count().from("ministry_roles").where("ministry_id=?", ministryId).search(searchParser.parse(search)).inOrg();
//        String sql = format("SELECT count(1) from ministry_roles WHERE ministry_id=?%s", optionalWhereClause(search));
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)){

            try(ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not get role roles with names containing " + search, e);
        }
    }

    public List<MinistryRole> getRoles(int ministryId, String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("ministry_roles").where("ministry_id=?", ministryId).search(searchParser.parse(search)).inOrg()
                .sort(sortField).limit(count).offset(start);
//        String sql = format("SELECT * FROM ministry_roles WHERE ministry_id=?%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get roles with names containing " + search, e);
        }
    }

    public MinistryRole getRole(int id) {
        QueryBuilder query = selectAll().from("ministry_roles").withId(id).inOrg();
        try(Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn) ) {

            List<MinistryRole> results = processResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get role by id: " + id, e);
        }
    }

    public void create(MinistryRole role) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministry_roles(name, ministry_id, contact, leader, org_id) values (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, role.getName());
            stmt.setInt(2, role.getMinistryId());
            stmt.setBoolean(3, role.isContact());
            stmt.setBoolean(4, role.isLeader());
            stmt.setInt(5, OrganizationContext.orgId());

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
              PreparedStatement stmt = conn.prepareStatement("UPDATE ministry_roles SET name=?, ministry_id=?, contact=?, leader=? WHERE id=? AND org_id=?")
        ){

            stmt.setString(1, role.getName());
            stmt.setInt(2, role.getMinistryId());
            stmt.setBoolean(3, role.isContact());
            stmt.setBoolean(4, role.isLeader());
            stmt.setInt(5, role.getId());
            stmt.setInt(6, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update role: " + role.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update role: " + role.getName(), e);
        }
    }

    public boolean delete(MinistryRole role) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministry_roles WHERE id=? AND org_id=?")
        ){

            stmt.setInt(1, role.getId());
            stmt.setInt(2, OrganizationContext.orgId());

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
