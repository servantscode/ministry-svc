package org.servantscode.ministry.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.ministry.MinistryRole;
import org.servantscode.ministry.db.RoleDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/ministry/{ministryId}/role")
public class MinistryRoleSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(MinistryRoleSvc.class);

    private final RoleDB db;
    private final int ministryId;

    public MinistryRoleSvc(@PathParam("ministryId") int ministryId) {
        this.db = new RoleDB();
        this.ministryId = ministryId;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<MinistryRole> getRoles(@QueryParam("start") @DefaultValue("0") int start,
                                           @QueryParam("count") @DefaultValue("20") int count,
                                           @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                           @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("ministry.role.list");
        try {
            LOG.trace(String.format("Retrieving ministry roles for ministry %d (%s, %s, page: %d; %d)", ministryId, nameSearch, sortField, start, count));
            int totalMinistries = db.getCount(ministryId, nameSearch);
            List<MinistryRole> results = db.getRoles(ministryId, nameSearch, sortField, start, count);
            return new PaginatedResponse<>(start, results.size(), totalMinistries, results);
        } catch (Throwable t) {
            LOG.error("Retrieving ministries failed:", t);
            throw new WebApplicationException("Retrieving ministries failed");
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public MinistryRole getRole(@PathParam("id") int id) {
        verifyUserAccess("ministry.role.read");
        try {
            MinistryRole role = db.getRole(id);
            if(role == null || role.getMinistryId() != ministryId)
                throw new NotFoundException();
            return role;
        } catch (Throwable t) {
            LOG.error("Retrieving ministry role failed:", t);
            throw new WebApplicationException("Retrieving ministry role failed");
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public MinistryRole createRole(MinistryRole role) {
        verifyUserAccess("ministry.role.create");

        if(role.getMinistryId() != ministryId)
            throw new BadRequestException();

        try {
            db.create(role);
            LOG.info("Created ministry role: " + role.getName());
            return role;
        } catch (Throwable t) {
            LOG.error("Creating ministry role failed:", t);
            throw new WebApplicationException("Creating ministry role failed");
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public MinistryRole updateRole(MinistryRole role) {
        verifyUserAccess("ministry.role.update");

        if(role.getMinistryId() != ministryId)
            throw new BadRequestException();

        MinistryRole dbRole = db.getRole(role.getId());
        if(dbRole.getId() != role.getId())
            throw new NotFoundException();

        try {
            db.update(role);
            LOG.info("Edited ministry role: " + role.getName());
            return role;
        } catch (Throwable t) {
            LOG.error("Updating ministry role failed:", t);
            throw new WebApplicationException("Updating ministry role failed");
        }
    }

    @DELETE @Path("/{id}")
    public void deleteRole(@PathParam("id") int id) {
        verifyUserAccess("ministry.role.delete");
        if(id <= 0)
            throw new NotFoundException();

        try {
            MinistryRole role = db.getRole(id);
            if(role == null )
                throw new NotFoundException();

            if(role.getMinistryId() != ministryId)
                throw new NotFoundException();

            if(!db.delete(role))
                throw new NotFoundException();

            LOG.info("Deleted ministry role: " + role.getName());
        } catch (Throwable t) {
            LOG.error("Deleting ministry role failed:", t);
            throw new WebApplicationException("Deleting ministry role failed");
        }
    }
}

