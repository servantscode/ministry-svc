package org.servantscode.ministry.db;

import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.ministry.MinistryEnrollment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EnrollmentDB extends EasyDB<MinistryEnrollment> {

    public EnrollmentDB() {
        super(MinistryEnrollment.class, "person_name");
    }

    private QueryBuilder baseQuery() {
        return select("e.*", "p.name AS person_name", "m.name AS ministry_name", "r.name AS role")
                .from("people p", "ministries m", "ministry_enrollments e")
                .join("LEFT JOIN ministry_roles r ON role_id=r.id")
                .where("p.org_id=?", OrganizationContext.orgId())
                .where("m.org_id=?", OrganizationContext.orgId())
                .where("r.org_id=?", OrganizationContext.orgId());
    }

    public List<MinistryEnrollment> getMinistryMembership(int ministryId) {
        return get(baseQuery().where("e.ministry_id=?", ministryId).where("p.id = person_id").where("m.id = e.ministry_id"));
    }

    public List<MinistryEnrollment> getPersonEnrollment(int personId) {
        return get(baseQuery().where("person_id=?", personId).where("p.id = person_id").where("m.id = e.ministry_id"));
    }

    public MinistryEnrollment populateEnrollment(MinistryEnrollment enrollment) {
        return getOne(baseQuery().where("person_id=?", enrollment.getPersonId())
                                 .where("e.ministry_id=?", enrollment.getMinistryId())
                                 .where("p.id = person_id").where("m.id = e.ministry_id"));
    }

    public boolean createEnrollment(MinistryEnrollment enrollment) {
        InsertBuilder cmd = insertInto("ministry_enrollments")
                .value("person_id", enrollment.getPersonId())
                .value("ministry_id", enrollment.getMinistryId())
                .value("role_id", enrollment.getRoleId());
        return create(cmd);
    }

    public boolean updateRole(MinistryEnrollment enrollment) {
        UpdateBuilder cmd = update("ministry_enrollments").value("role_id", enrollment.getRoleId())
                .with("person_id", enrollment.getPersonId())
                .with("ministry_id", enrollment.getMinistryId());
        return update(cmd);
    }

    public boolean deleteEnrollment(int personId, int ministryId) {
        return delete(deleteFrom("ministry_enrollments").with("person_id", personId).with("ministry_id", ministryId));
    }
    // ----- Private -----
    @Override
    protected MinistryEnrollment processRow(ResultSet rs) throws SQLException {
        MinistryEnrollment enrollment = new MinistryEnrollment(rs.getInt("person_id"),rs.getInt("ministry_id"), rs.getString("role"));
        enrollment.setRoleId(rs.getInt("role_id"));
        enrollment.setPersonName(rs.getString("person_name"));
        enrollment.setMinistryName(rs.getString("ministry_name"));
        return enrollment;
    }
}
