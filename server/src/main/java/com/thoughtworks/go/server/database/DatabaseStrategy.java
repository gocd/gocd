/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.SQLException;

@Component
public class DatabaseStrategy implements Database {
    private final Database database;

    @Autowired
    public DatabaseStrategy(SystemEnvironment systemEnvironment) {
        database = getDatabaseProvider(systemEnvironment);
    }

    @Override
    public String dialectForHibernate() {
        return database.dialectForHibernate();
    }

    @Override
    public String getType() {
        return database.getType();
    }

    @Override
    public void startDatabase() {
        database.startDatabase();
    }

    @Override
    public DataSource createDataSource() {
        return database.createDataSource();
    }

    @Override
    public void upgrade() throws SQLException {
        database.upgrade();
    }

    @Override
    public void shutdown() throws SQLException {
        database.shutdown();
    }

    @Override
    public void backup(File file) {
        database.backup(file);
    }

    @Override
    public String getIbatisConfigXmlLocation() {
        return database.getIbatisConfigXmlLocation();
    }

    @Override
    public QueryExtensions getQueryExtensions() {
        return database.getQueryExtensions();
    }

    private Database getDatabaseProvider(SystemEnvironment systemEnvironment) {
        String databaseProvider = systemEnvironment.getDatabaseProvider();
        try {
            Constructor<?> constructor = Class.forName(databaseProvider).getConstructor(SystemEnvironment.class);
            return ((Database) constructor.newInstance(systemEnvironment));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed loading database provider [%s]", databaseProvider), e);
        }
    }

    Database getDatabase() {
        return database;
    }
}
