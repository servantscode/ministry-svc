package org.servantscode.ministry.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Identity;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.ministry.Ministry;
import org.servantscode.ministry.db.MinistryDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

@Path("/ministry")
public class MinistrySvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(MinistrySvc.class);


    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "name", "description");
    public enum CONTACT_TYPE {CONTACTS, LEADERS, ALL};

    private final MinistryDB db;

    public MinistrySvc() {
        this.db = new MinistryDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Ministry> getMinistries(@QueryParam("start") @DefaultValue("0") int start,
                                           @QueryParam("count") @DefaultValue("100") int count,
                                           @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                           @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("ministry.list");
        try {
            LOG.trace(String.format("Retrieving ministries (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            int totalMinistries = db.getCount(nameSearch);
            List<Ministry> results = db.getMinistries(nameSearch, sortField, start, count);
            return new PaginatedResponse<>(start, results.size(), totalMinistries, results);
        } catch (Throwable t) {
            LOG.error("Retrieving ministries failed:", t);
            throw new WebApplicationException("Retrieving ministries failed");
        }
    }

    @GET @Path("/{id}/email/{contactType}") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getMinistryContacts(@PathParam("id") int ministryId,
                                            @PathParam("contactType") CONTACT_TYPE contactType) {
        verifyUserAccess("ministry.enrollment.list");
        verifyUserAccess("email.send");

        try {
            LOG.trace(String.format("Retrieving ministry emails (%s)", contactType));
            return db.getMinistryEmailList(ministryId, contactType);
        } catch (Throwable t) {
            LOG.error("Retrieving ministry email list failed:", t);
            throw new WebApplicationException("Retrieving ministry emails failed");
        }
    }

    @GET @Path("/{id}/contacts") @Produces(MediaType.APPLICATION_JSON)
    public List<Identity> getMinistryEmails(@PathParam("id") int ministryId) {
        verifyUserAccess("ministry.enrollment.list");

        try {
            LOG.trace("Retrieving ministry contacts");
            return db.getMinistryContactList(ministryId);
        } catch (Throwable t) {
            LOG.error("Retrieving ministry contact list failed:", t);
            throw new WebApplicationException("Retrieving ministry contact list failed.");
        }
    }


    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getReport(@QueryParam("search") @DefaultValue("") String nameSearch) {
        verifyUserAccess("ministry.export");

        try {
            LOG.trace(String.format("Retrieving ministry report(%s)", nameSearch));
            return Response.ok(db.getReportReader(nameSearch, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving ministry report failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Ministry getMinistry(@PathParam("id") int id) {
        verifyUserAccess("ministry.read");
        try {
            return db.getMinistry(id);
        } catch (Throwable t) {
            LOG.error("Retrieving ministry failed:", t);
            throw new WebApplicationException("Retrieving ministry failed");
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Ministry createMinistry(Ministry ministry) {
        verifyUserAccess("ministry.create");
        try {
            db.create(ministry);
            LOG.info("Created ministry: " + ministry.getName());
            return ministry;
        } catch (Throwable t) {
            LOG.error("Creating ministry failed:", t);
            throw new WebApplicationException("Creating ministry failed");
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Ministry updateMinistry(Ministry ministry) {
        verifyUserAccess("ministry.update");
        try {
            db.update(ministry);
            LOG.info("Edited ministry: " + ministry.getName());
            return ministry;
        } catch (Throwable t) {
            LOG.error("Updating ministry failed:", t);
            throw new WebApplicationException("Updating ministry failed");
        }
    }

    @DELETE @Path("/{id}")
    public void deleteMinistry(@PathParam("id") int id) {
        verifyUserAccess("ministry.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Ministry ministry = db.getMinistry(id);
            if(ministry == null || !db.delete(ministry))
                throw new NotFoundException();
            LOG.info("Deleted ministry: " + ministry.getName());
        } catch (Throwable t) {
            LOG.error("Deleting ministry failed:", t);
            throw new WebApplicationException("Deleting ministry failed");
        }
    }
}

