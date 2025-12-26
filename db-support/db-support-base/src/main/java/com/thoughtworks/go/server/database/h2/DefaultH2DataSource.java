/*
 * Copyright Thoughtworks, Inc.
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

package com.thoughtworks.go.server.database.h2;

import com.thoughtworks.go.server.database.DbProperties;
import lombok.experimental.UtilityClass;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.File;

@UtilityClass
public class DefaultH2DataSource {

    private static final String DEFAULT_DB_DIRECTORY = "./db/h2db";
    private static final String DEFAULT_DB_NAME = "cruise";
    private static final int DEFAULT_CACHE_SIZE_IN_KB_PER_GB_OF_RAM = 128 * 1024; // 128MB

    public static BasicDataSource forBasicConnection(DbProperties properties) {
        BasicDataSource basicDataSource = new BasicDataSource();
        File defaultDbDir = new File(DEFAULT_DB_DIRECTORY);
        if (!defaultDbDir.exists()) {
            defaultDbDir.mkdirs();
        }
        // FIXME review all these H2 settings/defaults

        String defaultH2Url = String.join(";",
            "jdbc:h2:file:" + DEFAULT_DB_DIRECTORY + '/' + DEFAULT_DB_NAME,
            "DB_CLOSE_DELAY=-1", // Constants.DB_CLOSE_DELAY
            "DB_CLOSE_ON_EXIT=FALSE", // do not close the DB on JVM exit
            "CACHE_SIZE=" + DEFAULT_CACHE_SIZE_IN_KB_PER_GB_OF_RAM
        );

        basicDataSource.setUrl(defaultH2Url);
        basicDataSource.setUsername(properties.user().isBlank() ? "sa" : properties.user());
        basicDataSource.setPassword(properties.password().strip());
        return basicDataSource;
    }
}
