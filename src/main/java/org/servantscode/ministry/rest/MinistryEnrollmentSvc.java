package org.servantscode.ministry.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.ministry.Ministry;
import org.servantscode.ministry.MinistryEnrollment;
import org.servantscode.ministry.db.EnrollmentDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.Arrays.asList;

@Path("/enrollment")
public class MinistryEnrollmentSvc extends SCServiceBase {
    private static final Logger logger = LogManager.getLogger(MinistryEnrollmentSvc.class);

    EnrollmentDB db;
    public MinistryEnrollmentSvc() {
        db = new EnrollmentDB();
    }

    @GET @Path("/person/{personId}")@Produces(MediaType.APPLICATION_JSON)
    public List<MinistryEnrollment> getMinistryEnrollments(@PathParam("personId") @DefaultValue("0") int personId) {
        verifyUserAccess("ministry.enrollment.list");
        try {
            logger.trace(String.format("Retrieving ministry enrollments. Person: %d", personId));
            return db.getPersonEnrollment(personId);
        } catch (Throwable t) {
            logger.error("Enrollment retrieval failed:", t);
            throw new WebApplicationException("Enrollment retrieval failed");
        }
    }

    @GET @Path("/ministry/{ministryId}") @Produces(MediaType.APPLICATION_JSON)
    public List<MinistryEnrollment> getMinistryMembership(@PathParam("ministryId") @DefaultValue("0") int ministryId) {

        verifyUserAccess("ministry.enrollment.list");
        try {
            logger.trace(String.format("Retrieving ministry enrollments. Ministry: %d", ministryId));
            return db.getMinistryMembership(ministryId);
        } catch (Throwable t) {
            logger.error("Enrollment retrieval failed:", t);
            throw new WebApplicationException("Enrollment retrieval failed");
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public MinistryEnrollment createEnrollment(MinistryEnrollment enrollment) {
        verifyUserAccess("ministry.enrollment.create");

        if(enrollment.getRoleId() <= 0 || enrollment.getPersonId() <= 0 || enrollment.getMinistryId() <= 0)
            throw new BadRequestException();

        try {
            db.createEnrollment(enrollment);
            logger.info(String.format("Enrolled person %d in ministry %d with role %s", enrollment.getPersonId(), enrollment.getMinistryId(), enrollment.getRole()));
            return db.populateEnrollment(enrollment);
        } catch (Throwable t) {
            logger.error("Enrollment failed:", t);
            throw new WebApplicationException("Enrollment failed");
        }
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getRelationshipReport() {

        verifyUserAccess("ministry.enrollment..list");

        try {
            logger.trace(String.format("Retrieving ministry enrollment report"));
            List<String> EXPORTABLE_FIELDS = asList("person_id", "person_name", "ministry_id", "ministry_name", "role_id", "role");
            return Response.ok(db.getReportReader(EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            logger.error("Retrieving people report failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public MinistryEnrollment updateRole(MinistryEnrollment enrollment) {
        verifyUserAccess("ministry.enrollment.update");

        if(enrollment.getRoleId() <= 0 || enrollment.getPersonId() <= 0 || enrollment.getMinistryId() <= 0)
            throw new BadRequestException();

        try {
            if(db.updateRole(enrollment)) {
                logger.info(String.format("Updated person %d in ministry %d to role %s", enrollment.getPersonId(), enrollment.getMinistryId(), enrollment.getRole()));
                return db.populateEnrollment(enrollment);
            } else {
                throw new NotFoundException();
            }
        } catch (Throwable t) {
            logger.error("Role update failed:", t);
            throw new WebApplicationException("Role update failed");
        }
    }

    @DELETE @Path("/ministry/{ministryId}/person/{personId}") @Consumes(MediaType.APPLICATION_JSON)
    public void deleteEnrollment(@PathParam("ministryId") int ministryId,
                                 @PathParam("personId") int personId) {
        verifyUserAccess("ministry.enrollment.delete");
        if(personId <= 0 || ministryId <= 0)
            throw new NotFoundException();

        try {
            if(!db.deleteEnrollment(personId, ministryId))
                throw new NotFoundException();
            logger.info(String.format("Removed person %d from ministry %d", personId, ministryId));
        } catch (Throwable t) {
            logger.error("De-enrollment failed:", t);
            throw new WebApplicationException("De-enrollment failed");
        }
    }
}

