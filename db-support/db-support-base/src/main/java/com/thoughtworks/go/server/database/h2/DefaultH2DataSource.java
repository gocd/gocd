/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class DefaultH2DataSource {
    public static BasicDataSource defaultH2DataSource(BasicDataSource basicDataSource, DbProperties properties) {
        File defaultDbDir = new File("db/h2db");
        if (!defaultDbDir.exists()) {
            defaultDbDir.mkdirs();
        }

        String defaultCacheSizeInMB = String.valueOf(128 * 1024); //128MB
        String defaultH2Url = "jdbc:h2:./db/h2db/cruise" +
                ";DB_CLOSE_DELAY=-1" +
                ";DB_CLOSE_ON_EXIT=FALSE" + // do not close the DB on JVM exit
                ";CACHE_SIZE=" + defaultCacheSizeInMB +
                ";TRACE_MAX_FILE_SIZE=16" + // http://www.h2database.com/html/features.html#trace_options
                ";TRACE_LEVEL_FILE=1" // http://www.h2database.com/html/features.html#trace_options
                ;

        basicDataSource.setUrl(defaultH2Url);
        basicDataSource.setUsername(StringUtils.defaultIfBlank(properties.user(), "sa"));
        basicDataSource.setPassword(StringUtils.stripToEmpty(properties.password()));
        basicDataSource.setMaxIdle(properties.maxIdle());
        basicDataSource.setMaxTotal(properties.maxTotal());
        return basicDataSource;
    }
}
