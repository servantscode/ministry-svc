package org.servantscode.ministry.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.AbstractDBUpgrade;

import java.sql.SQLException;

public class DBUpgrade extends AbstractDBUpgrade {
    private static final Logger LOG = LogManager.getLogger(DBUpgrade.class);

    @Override
    public void doUpgrade() throws SQLException {
        LOG.info("Verifying database structures.");

        if(!tableExists("ministries")) {
            LOG.info("-- Creating ministries table");
            runSql("CREATE TABLE ministries (id SERIAL PRIMARY KEY, " +
                                            "name TEXT, " +
                                            "description TEXT, " +
                                            "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("ministry_roles")) {
            LOG.info("-- Creating ministry_roles table");
            runSql("CREATE TABLE ministry_roles (id SERIAL PRIMARY KEY, " +
                                                "ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE NOT NULL, " +
                                                "name TEXT NOT NULL, " +
                                                "contact BOOLEAN, " +
                                                "leader BOOLEAN, " +
                                                "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("ministry_enrollments")) {
            LOG.info("-- Creating ministry_enrollments table");
            runSql("CREATE TABLE ministry_enrollments (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, " +
                                                      "ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, " +
                                                      "role_id INTEGER REFERENCES ministry_roles(id) ON DELETE SET NULL)");
        }
    }
}
