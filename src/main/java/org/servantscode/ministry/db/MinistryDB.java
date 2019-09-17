package org.servantscode.ministry.db;

import org.servantscode.commons.Identity;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.search.UpdateBuilder;
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

public class MinistryDB extends EasyDB<Ministry> {

    public MinistryDB() {
        super(Ministry.class, "name");
    }

    public int getCount(String search) {
        return getCount(count().from("ministries").search(searchParser.parse(search)).inOrg());
    }

    public List<Ministry> getMinistries(String search, String sortField, int start, int count) {
        return get(selectAll().from("ministries").search(searchParser.parse(search)).inOrg().page(sortField, start, count));
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        final QueryBuilder query = selectAll().from("ministries").search(searchParser.parse(search)).inOrg();
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
        return getOne(selectAll().from("ministries").withId(id).inOrg());
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

    public List<Identity> getMinistryContactList(int ministryId) {
        QueryBuilder query = select("p.id", "p.name").from("people p", "ministry_enrollments e", "ministry_roles r")
                .where("p.id=e.person_id").where("e.role_id=r.id").where("r.contact=true").where("e.ministry_id=?", ministryId);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)) {

            List<Identity> emails = new LinkedList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    emails.add(new Identity(rs.getString("name"), rs.getInt("id")));
            }
            return emails;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get ministry email list by id: " + ministryId, e);
        }
    }

    public void create(Ministry ministry) {
        InsertBuilder cmd = insertInto("ministries")
                .value("name", ministry.getName())
                .value("description", ministry.getDescription())
                .value("org_id", OrganizationContext.orgId());
        ministry.setId(createAndReturnKey(cmd));
    }

    public void update(Ministry ministry) {
        UpdateBuilder cmd = update("ministries")
                .value("name", ministry.getName())
                .value("description", ministry.getDescription())
                .withId(ministry.getId())
                .inOrg();
        if(!update(cmd))
            throw new RuntimeException("Could not update ministry: " + ministry.getName());
    }

    public boolean delete(Ministry ministry) {
        return delete(deleteFrom("ministries").withId(ministry.getId()).inOrg());
    }

    // ----- Private ------
    @Override
    protected Ministry processRow(ResultSet rs) throws SQLException {
        Ministry ministry = new Ministry(rs.getInt("id"), rs.getString("name"));
        ministry.setDescription(rs.getString("description"));
        return ministry;
    }
}
