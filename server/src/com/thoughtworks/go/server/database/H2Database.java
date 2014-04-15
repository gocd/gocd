/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.database;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.database.QueryExtensions;
import com.thoughtworks.go.server.util.H2EventListener;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.h2.tools.Server;

import static com.thoughtworks.go.server.util.SqlUtil.joinWithQuotesForSql;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands database administration
 */
public class H2Database implements Database {
    private static final Logger LOG = Logger.getLogger(H2Database.class);
    private SystemEnvironment env;
    private BasicDataSource dataSource;
    private Server tcpServer;
    static final String DIALECT_H2 = "org.hibernate.dialect.H2Dialect";

    public H2Database(SystemEnvironment env) {
        this.env = env;
    }

    @Override
    public String dialectForHibernate() {
        return DIALECT_H2;
    }

    @Override
    public String getType() {
        return "h2";
    }

    public void startDatabase() {
        try {
            new Migrate().execute(env.getDbPath(), true, Migrate.USER, Migrate.PASSWORD, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (env.inDbDebugMode()) {
            if (tcpServer != null) {
                return;
            }
            try {
                DataSource ds = createDataSource();
                Connection con = ds.getConnection();
                ResultSet set = con.getMetaData().getTables(null, null, null, null);
                set.next();
                set.close();
                con.close();
                LOG.info("Database is already running.");
                return;
            } catch (Exception e) {
                LOG.info("Database is not running - starting a new one.");
            }
            try {
                LOG.info("Starting h2 server in debug mode : "
                        + "port=" + dbPortAsString()
                        + " baseDir=" + env.getDbPath().getCanonicalPath());
                String[] args = {
                        "-tcp",
                        "-tcpAllowOthers",
                        "-tcpPort", dbPortAsString(),
                        "-baseDir", env.getDbPath().getCanonicalPath()
                };
                tcpServer = Server.createTcpServer(args);
                tcpServer.start();
            } catch (Exception e) {
                bomb("Could not create database server.", e);
            }
        }
    }


    private BasicDataSource createDataSource(Boolean mvccEnabled) {
        if (this.dataSource == null) {
            BasicDataSource source = new BasicDataSource();
            if (env.inDbDebugMode()) {
                String url = "jdbc:h2:tcp://localhost:" + dbPortAsString() + "/cruise";
                configureDataSource(source, url);
                LOG.info("Creating debug datasource on port=" + dbPortAsString());
            } else {
                String url = dburl(mvccEnabled);
                configureDataSource(source, url);
                LOG.info("Creating datasource with url=" + url);
            }
            this.dataSource = source;
        }
        return dataSource;
    }

    private void configureDataSource(BasicDataSource source, String url) {
        String databaseUsername = env.get(SystemEnvironment.GO_DATABASE_USER);
        String databasePassword = env.get(SystemEnvironment.GO_DATABASE_PASSWORD);
        LOG.info(String.format("[db] Using connection configuration %s [User: %s]", url, databaseUsername));
        source.setDriverClassName("org.h2.Driver");
        source.setUrl(url);
        source.setUsername(databaseUsername);
        source.setPassword(databasePassword);
        source.setMaxActive(env.get(SystemEnvironment.GO_DATABASE_MAX_ACTIVE));
        source.setMaxIdle(env.get(SystemEnvironment.GO_DATABASE_MAX_IDLE));
    }

    public BasicDataSource createDataSource() {
        return createDataSource(Boolean.TRUE);
    }

    private String dbPortAsString() {
        return String.valueOf(env.getDatabaseSeverPort());
    }

    private String dburl(Boolean mvccEnabled) {
        return "jdbc:h2:" + env.getDbPath() + "/" + env.get(SystemEnvironment.GO_DATABASE_NAME)
                + ";DB_CLOSE_DELAY=-1"
                + ";DB_CLOSE_ON_EXIT=FALSE"
                + ";MVCC=" + mvccEnabled.toString().toUpperCase()
                + ";CACHE_SIZE=" + env.getCruiseDbCacheSize()
                + ";TRACE_LEVEL_FILE=" + env.getCruiseDbTraceLevel()
                + ";TRACE_MAX_FILE_SIZE=" + env.getCruiseDbTraceFileSize()
//                Commented out until H2 fix their bug
//                + ";CACHE_TYPE=SOFT_LRU" //See http://www.h2database.com/html/changelog.html
                + ";DATABASE_EVENT_LISTENER='" + H2EventListener.class.getName() + "'";
    }

    public void upgrade() throws SQLException {
        BasicDataSource source = createDataSource(Boolean.FALSE);
        if (env.inDbDebugMode()) {
            LOG.info("In debug mode - not upgrading database");
            //don't upgrade
        } else {
            Migration upgradeToH2 = new MigrateHsqldbToH2(source, env);
            upgradeToH2.migrate();

            Migration migrateSchema = new DbDeployMigration(source, env);
            migrateSchema.migrate();
        }
        shutdown();
    }

    public void shutdown() throws SQLException {
        if (env.inDbDebugMode()) {
            LOG.info("Shutting down database server.");
            if (tcpServer == null) {
                return;
            }
            if (dataSource != null) {
                dataSource.close();
            }
            dataSource = null;
            tcpServer.stop();
            tcpServer = null;
        } else {
            Connection connection = createDataSource().getConnection();
            Statement statement = connection.createStatement();
            statement.execute("SHUTDOWN");
            statement.close();
            dataSource.close();
            dataSource = null;
        }
    }

    public void backup(File file) {
        Connection connection = null;
        try {
            connection = createDataSource().getConnection();
            Statement statement = connection.createStatement();
            File dbBackupFile = new File(file, "db.zip");
            statement.execute(String.format("BACKUP TO '%s'", dbBackupFile));
        } catch (SQLException e) {
            bomb(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public String getIbatisConfigXmlLocation() {
        return null;
    }

    @Override
    public QueryExtensions getQueryExtensions() {
        return new QueryExtensions() {
            @Override
            public String queryFromInclusiveModificationsForPipelineRange(String pipelineName, Integer fromCounter, Integer toCounter) {
                return "WITH LINK(id) AS ( "
                        + "  SELECT id "
                        + "     FROM pipelines "
                        + "     WHERE name = " + org.h2.util.StringUtils.quoteStringSQL(
                        pipelineName)  // using string concatenation because Hibernate does not seem to be able to replace named or positional parameters here
                        + "         AND counter >= " + fromCounter
                        + "         AND counter <= " + toCounter
                        + "  UNION ALL "
                        + "  SELECT mod.pipelineId "
                        + "     FROM link "
                        + "         INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId "
                        + "         INNER JOIN modifications mod ON pmr.toRevisionId >= mod.id and pmr.actualFromRevisionId <= mod.id AND pmr.materialId = mod.materialId "
                        + "     WHERE mod.pipelineId IS NOT NULL"
                        + ")"
                        + "SELECT DISTINCT id FROM link WHERE id IS NOT NULL";

            }

            @Override
            public String queryRelevantToLookedUpDependencyMap(List<Long> pipelineIds) {
                return "WITH LINK(id, name, lookedUpId) AS ( "
                        + "  SELECT id, name, id as lookedUpId"
                        + "     FROM pipelines "
                        + "     WHERE id in (" + joinWithQuotesForSql(pipelineIds.toArray()) + ") "
                        + "  UNION ALL "
                        + "  SELECT mod.pipelineId as id, p.name as name, link.lookedUpId as lookedUpId "
                        + "     FROM link "
                        + "         INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId "
                        + "         INNER JOIN modifications mod ON pmr.toRevisionId >= mod.id and pmr.actualFromRevisionId <= mod.id AND pmr.materialId = mod.materialId "
                        + "         INNER JOIN pipelines p ON mod.pipelineId = p.id "
                        + "     WHERE mod.pipelineId IS NOT NULL"
                        + ")"
                        + "SELECT id, name, lookedUpId FROM link";
            }
        };
    }
}
