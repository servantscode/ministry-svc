package org.servantscode.ministry.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.ministry.Ministry;
import org.servantscode.ministry.db.MinistryDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/ministry")
public class MinistrySvc {
    private static final Logger logger = LogManager.getLogger(MinistrySvc.class);

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getMinistryNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            logger.trace(String.format("Retrieving ministry names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            MinistryDB db = new MinistryDB();
            return db.getMinistryNames(nameSearch, count);
        } catch (Throwable t) {
            logger.error("Retrieving ministries failed:", t);
            throw new WebApplicationException("Retrieving ministries failed");
        }
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Ministry> getMinistries(@QueryParam("start") @DefaultValue("0") int start,
                                           @QueryParam("count") @DefaultValue("100") int count,
                                           @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                           @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            logger.trace(String.format("Retrieving ministries (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            MinistryDB db = new MinistryDB();
            int totalMinistries = db.getCount(nameSearch);
            List<Ministry> results = db.getMinistries(nameSearch, sortField, start, count);
            return new PaginatedResponse<>(start, results.size(), totalMinistries, results);
        } catch (Throwable t) {
            logger.error("Retrieving ministries failed:", t);
            throw new WebApplicationException("Retrieving ministries failed");
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Ministry getMinistry(@PathParam("id") int id) {
        try {
            return new MinistryDB().getMinistry(id);
        } catch (Throwable t) {
            logger.error("Retrieving ministry failed:", t);
            throw new WebApplicationException("Retrieving ministry failed");
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Ministry createMinistry(Ministry ministry) {
        try {
            new MinistryDB().create(ministry);
            logger.info("Created ministry: " + ministry.getName());
            return ministry;
        } catch (Throwable t) {
            logger.error("Creating ministry failed:", t);
            throw new WebApplicationException("Creating ministry failed");
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Ministry updateMinistry(Ministry ministry) {
        try {
            new MinistryDB().update(ministry);
            logger.info("Edited ministry: " + ministry.getName());
            return ministry;
        } catch (Throwable t) {
            logger.error("Updating ministry failed:", t);
            throw new WebApplicationException("Updating ministry failed");
        }
    }

    @DELETE @Path("/{id}")
    public void deleteMinistry(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Ministry ministry = new MinistryDB().getMinistry(id);
            if(ministry == null || new MinistryDB().delete(ministry))
                throw new NotFoundException();
            logger.info("Deleted ministry: " + ministry.getName());
        } catch (Throwable t) {
            logger.error("Deleting ministry failed:", t);
            throw new WebApplicationException("Deleting ministry failed");
        }
    }
}

