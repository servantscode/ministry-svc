package org.servantscode.ministry.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.ministry.Ministry;
import org.servantscode.ministry.rest.MinistrySvc;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class MinistryDB extends DBAccess {

    public int getCount(String search) {
        String sql = format("Select count(1) from ministries%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery() ){

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public List<Ministry> getMinistries(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM ministries%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processMinistryResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        final String sql = format("SELECT * FROM ministries p%s", optionalWhereClause(search));

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve ministries containing '" + search + "'", e);
                }
            }
        };
    }

    public Ministry getMinistry(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ministries WHERE id=?")
        ) {

            stmt.setInt(1, id);
            List<Ministry> results = processMinistryResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry by id: " + id, e);
        }
    }

    public List<String> getMinistryEmailList(int ministryId, MinistrySvc.CONTACT_TYPE contactType) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT p.email " +
                      "FROM people p, ministry_enrollments e, ministry_roles r " +
                      "WHERE p.id = e.person_id AND e.ministry_id=? AND e.role_id=r.id" + optionalContactFilter(contactType))) {

            stmt.setInt(1, ministryId);
            List<String> emails = new LinkedList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    emails.add(rs.getString("email"));
            }
            return emails;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry email list by id: " + ministryId, e);
        }
    }

    public void create(Ministry ministry) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministries(name, description) values (?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create ministry: " + ministry.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    ministry.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create ministry: " + ministry.getName(), e);
        }
    }

    public void update(Ministry ministry) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE ministries SET name=?, description=? WHERE id=?")
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());
            stmt.setInt(3, ministry.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update ministry: " + ministry.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update ministry: " + ministry.getName(), e);
        }
    }

    public boolean delete(Ministry ministry) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministries WHERE id=?")
        ){

            stmt.setInt(1, ministry.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete ministry: " + ministry.getName(), e);
        }
    }

    // ----- Private ------
    private List<Ministry> processMinistryResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Ministry> ministries = new ArrayList<>();
            while(rs.next()) {
                Ministry ministry = new Ministry(rs.getInt("id"), rs.getString("name"));
                ministry.setDescription(rs.getString("description"));
                ministries.add(ministry);
            }
            return ministries;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search)? format(" WHERE name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }

    private String optionalContactFilter(MinistrySvc.CONTACT_TYPE contactType) {
        switch (contactType) {
            case CONTACTS:
                return " AND r.contact=true";
            case LEADERS:
                return " AND r.leader=true";
            case ALL:
                return "";
            default:
                throw new IllegalArgumentException();
        }
    }

}
