package org.servantscode.ministry.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
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

import static org.servantscode.ministry.rest.MinistrySvc.CONTACT_TYPE.CONTACTS;
import static org.servantscode.ministry.rest.MinistrySvc.CONTACT_TYPE.LEADERS;

public class MinistryDB extends DBAccess {

    private SearchParser<Ministry> searchParser;

    public MinistryDB() {
        searchParser = new SearchParser<>(Ministry.class, "name");
    }

    public int getCount(String search) {
        QueryBuilder query = count().from("ministries").search(searchParser.parse(search)).inOrg();
//        String sql = format("Select count(1) from ministries%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery() ){

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public List<Ministry> getMinistries(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("ministries").search(searchParser.parse(search)).inOrg()
                .sort(sortField).limit(count).offset(start);
//        String sql = format("SELECT * FROM ministries%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            return processMinistryResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministries with names containing " + search, e);
        }
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        final QueryBuilder query = selectAll().from("ministries").search(searchParser.parse(search)).inOrg();
//        final String sql = format("SELECT * FROM ministries p%s", optionalWhereClause(search));

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = query.prepareStatement(conn);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve ministries containing '" + search + "'", e);
                }
            }
        };
    }

    public Ministry getMinistry(int id) {
        QueryBuilder query = selectAll().from("ministries").withId(id).inOrg();
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            List<Ministry> results = processMinistryResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry by id: " + id, e);
        }
    }

    public List<String> getMinistryEmailList(int ministryId, MinistrySvc.CONTACT_TYPE contactType) {
        QueryBuilder query = select("p.email").from("people p", "ministry_enrollments e", "ministry_roles r")
                .where("p.id=e.person_id").where("e.ministry_id=?", ministryId).where("e.role_id=r.id");

        if(contactType == CONTACTS)
            query.where("r.contact=true");
        if(contactType == LEADERS)
            query.where("r.leader=true");

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)) {

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
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministries(name, description, org_id) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());
            stmt.setInt(3, OrganizationContext.orgId());

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
              PreparedStatement stmt = conn.prepareStatement("UPDATE ministries SET name=?, description=? WHERE id=? AND org_id=?")
        ){

            stmt.setString(1, ministry.getName());
            stmt.setString(2, ministry.getDescription());
            stmt.setInt(3, ministry.getId());
            stmt.setInt(4, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update ministry: " + ministry.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update ministry: " + ministry.getName(), e);
        }
    }

    public boolean delete(Ministry ministry) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministries WHERE id=? AND org_id=?")
        ){

            stmt.setInt(1, ministry.getId());
            stmt.setInt(2, OrganizationContext.orgId());

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
}
