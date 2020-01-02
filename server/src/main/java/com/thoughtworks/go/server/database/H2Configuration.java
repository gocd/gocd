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

import com.thoughtworks.go.util.SystemEnvironment;

/*
    Responsible for loading H2 Configuration.
 */
public class H2Configuration  {

    private SystemEnvironment systemEnvironment;

    public H2Configuration(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public String getHost() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_HOST);
    }

    public int getPort() {
        return systemEnvironment.getDatabaseSeverPort();
    }

    public String getName() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_NAME);
    }

    public String getUser() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_USER);
    }

    public String getPassword() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_PASSWORD);
    }

    public int getMaxActive() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_MAX_ACTIVE);
    }

    public int getMaxIdle() {
        return systemEnvironment.get(SystemEnvironment.GO_DATABASE_MAX_IDLE);
    }

}
