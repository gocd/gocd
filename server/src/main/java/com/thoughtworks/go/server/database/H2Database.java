/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.database;

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.database.QueryExtensions;
import com.thoughtworks.go.server.util.H2EventListener;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands database administration
 */
public class H2Database implements Database {
    private static final Logger LOG = LoggerFactory.getLogger(H2Database.class);
    static final String DIALECT_H2 = "org.hibernate.dialect.H2Dialect";

    private final H2Configuration configuration;
    private final SystemEnvironment systemEnvironment;
    private BasicDataSource dataSource;
    private Server tcpServer;

    public H2Database(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        this.configuration = new H2Configuration(systemEnvironment);
    }

    @Override
    public String dialectForHibernate() {
        return DIALECT_H2;
    }

    @Override
    public String getType() {
        return "h2";
    }

    @Override
    public void startDatabase() {
        if (systemEnvironment.inDbDebugMode()) {
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
                        + "port=" + configuration.getPort()
                        + " baseDir=" + systemEnvironment.getDbPath().getCanonicalPath());
                String[] args = {
                        "-tcp",
                        "-tcpAllowOthers",
                        "-tcpPort", String.valueOf(configuration.getPort()),
                        "-baseDir", systemEnvironment.getDbPath().getCanonicalPath()
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
            if (systemEnvironment.inDbDebugMode()) {
                String url = String.format("jdbc:h2:tcp://%s:%s/%s", configuration.getHost(),
                        configuration.getPort(), configuration.getName());
                configureDataSource(source, url);
                LOG.info("Creating debug data source on port={}", configuration.getPort());
            } else {
                String url = dburl(mvccEnabled);
                configureDataSource(source, url);
                LOG.info("Creating data source with url={}", url);
            }
            this.dataSource = source;
        }
        return dataSource;
    }

    private void configureDataSource(BasicDataSource source, String url) {
        String databaseUsername = configuration.getUser();
        String databasePassword = configuration.getPassword();
        LOG.info("[db] Using connection configuration {} [User: {}]", url, databaseUsername);
        source.setDriverClassName("org.h2.Driver");
        source.setUrl(url);
        source.setUsername(databaseUsername);
        source.setPassword(databasePassword);
        source.setMaxTotal(configuration.getMaxActive());
        source.setMaxIdle(configuration.getMaxIdle());
    }

    @Override
    public BasicDataSource createDataSource() {
        return createDataSource(Boolean.TRUE);
    }

    private String dburl(Boolean mvccEnabled) {
        return "jdbc:h2:" + systemEnvironment.getDbPath().getAbsolutePath() + "/" + configuration.getName()
                + ";DB_CLOSE_DELAY=-1"
                + ";DB_CLOSE_ON_EXIT=FALSE"
                + ";MVCC=" + mvccEnabled.toString().toUpperCase()
                + ";CACHE_SIZE=" + systemEnvironment.getCruiseDbCacheSize()
                + ";TRACE_LEVEL_FILE=" + systemEnvironment.getCruiseDbTraceLevel()
                + ";TRACE_MAX_FILE_SIZE=" + systemEnvironment.getCruiseDbTraceFileSize()
//                Commented out until H2 fix their bug
//                + ";CACHE_TYPE=SOFT_LRU" //See http://www.h2database.com/html/changelog.html
                + ";DATABASE_EVENT_LISTENER='" + H2EventListener.class.getName() + "'";
    }

    @Override
    public void upgrade() throws SQLException {
        BasicDataSource source = createDataSource(Boolean.FALSE);
        if (systemEnvironment.inDbDebugMode()) {
            LOG.info("In debug mode - not upgrading database");
            //don't upgrade
        } else {
            Migration migrateSchema = new DbDeployMigration(source, systemEnvironment);
            migrateSchema.migrate();
        }
        shutdown();
    }

    @Override
    public void shutdown() throws SQLException {
        if (systemEnvironment.inDbDebugMode()) {
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

    @Override
    public void backup(File file) {
        try (Connection connection = createDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            File dbBackupFile = new File(file, "db.zip");
            statement.execute(String.format("BACKUP TO '%s'", dbBackupFile));
        } catch (SQLException e) {
            bomb(e);
        }
        // Ignore
    }

    @Override
    public String getIbatisConfigXmlLocation() {
        return null;
    }

    @Override
    public QueryExtensions getQueryExtensions() {
        return new H2QueryExtensions();
    }

}
