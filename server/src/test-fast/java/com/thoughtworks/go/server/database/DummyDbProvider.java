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

import java.io.File;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.database.QueryExtensions;
import com.thoughtworks.go.util.SystemEnvironment;

public class DummyDbProvider implements Database {
    public boolean startDatabase;
    public boolean createDataSource;
    public boolean upgrade;
    public boolean shutdown;
    public boolean backup;
    public boolean getIbatisConfigXmlLocation;
    public boolean getQueryExtensions;

    public DummyDbProvider(SystemEnvironment environment) {
    }

    @Override
    public String dialectForHibernate() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public void startDatabase() {
        this.startDatabase = true;
    }

    @Override
    public DataSource createDataSource() {
        this.createDataSource = true;
        return null;
    }

    @Override
    public void upgrade() throws SQLException {
        this.upgrade = true;
    }

    @Override
    public void shutdown() throws SQLException {
        this.shutdown = true;
    }

    @Override
    public void backup(File file) {
        this.backup = true;
    }

    @Override
    public String getIbatisConfigXmlLocation() {
        this.getIbatisConfigXmlLocation = true;
        return null;
    }

    @Override
    public QueryExtensions getQueryExtensions() {
        this.getQueryExtensions = true;
        return null;
    }
}
