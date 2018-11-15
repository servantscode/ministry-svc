package org.servantscode.ministry.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.ministry.MinistryEnrollment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDB extends DBAccess {

    public List<MinistryEnrollment> getMinistryMembmership(int ministryId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT e.*, p.name AS person_name, m.name AS ministry_name " +
                                                                 "FROM ministry_enrollments e, people p, ministries m " +
                                                                 "WHERE ministry_id=? AND p.id = person_id AND m.id = ministry_id");
        ){

            stmt.setInt(1, ministryId);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve memebership for ministry with id: " + ministryId, e);
        }
    }

    public List<MinistryEnrollment> getPersonEnrollment(int personId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT e.*, p.name AS person_name, m.name AS ministry_name " +
                                                                 "FROM ministry_enrollments e, people p, ministries m " +
                                                                 "WHERE person_id=? AND p.id = person_id AND m.id = ministry_id");
        ){

            stmt.setInt(1, personId);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve ministry enrollments for person with id: " + personId, e);
        }
    }

    public MinistryEnrollment populateEnrollment(MinistryEnrollment enrollment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT e.*, p.name AS person_name, m.name AS ministry_name " +
                                                                 "FROM ministry_enrollments e, people p, ministries m " +
                                                                 "WHERE person_id=? AND ministry_id=? AND p.id = person_id AND m.id = ministry_id");
        ){

            stmt.setInt(1, enrollment.getPersonId());
            stmt.setInt(2, enrollment.getMinistryId());

            return processResults(stmt).get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not populate enrollment for person with id: " + enrollment.getPersonId() + " and ministry with id: " + enrollment.getMinistryId(), e);
        }
    }

    public boolean createEnrollment(MinistryEnrollment enrollment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO ministry_enrollments(person_id, ministry_id, role) VALUES (?, ?, ?)");
        ){

            stmt.setInt(1, enrollment.getPersonId());
            stmt.setInt(2, enrollment.getMinistryId());
            stmt.setString(3, enrollment.getRole());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create enrollment: Person(" + enrollment.getPersonId() + "), Ministry(" + enrollment.getMinistryId() + "): " + enrollment.getRole(), e);
        }
    }

    public boolean updateRole(MinistryEnrollment enrollment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE ministry_enrollments SET role=? WHERE person_id =? AND ministry_id =?");
        ){

            stmt.setString(1, enrollment.getRole());
            stmt.setInt(2, enrollment.getPersonId());
            stmt.setInt(3, enrollment.getMinistryId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create enrollment: Person(" + enrollment.getPersonId() + "), Ministry(" + enrollment.getMinistryId() + "): " + enrollment.getRole(), e);
        }
    }

    public boolean deleteEnrollment(MinistryEnrollment enrollment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM ministry_enrollments WHERE person_id = ? AND ministry_id =?");
        ){

            stmt.setInt(1, enrollment.getPersonId());
            stmt.setInt(2, enrollment.getMinistryId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create enrollment: Person(" + enrollment.getPersonId() + "), Ministry(" + enrollment.getMinistryId() + "): " + enrollment.getRole(), e);
        }
    }
    // ----- Private -----
    private List<MinistryEnrollment> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<MinistryEnrollment> ministries = new ArrayList<>();
            while(rs.next()) {
                MinistryEnrollment enrollment = new MinistryEnrollment(rs.getInt("person_id"),rs.getInt("ministry_id"), rs.getString("role"));
                enrollment.setPersonName(rs.getString("person_name"));
                enrollment.setMinistryName(rs.getString("ministry_name"));
                ministries.add(enrollment);
            }
            return ministries;
        }
    }
}
