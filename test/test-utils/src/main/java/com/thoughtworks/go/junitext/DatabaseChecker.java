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
package com.thoughtworks.go.junitext;

import com.googlecode.junit.ext.checkers.Checker;
import com.thoughtworks.go.util.SystemEnvironment;

public class DatabaseChecker implements Checker {
    public static final String H2 = "H2Database";
    private String targetDB;

    public DatabaseChecker(String targetDB) {
        this.targetDB = H2;
        if (targetDB != null) {
            this.targetDB = targetDB;
        }
    }

    @Override
    public boolean satisfy() {
        String databaseProvider = System.getProperty("go.database.provider", SystemEnvironment.H2_DATABASE);

        if (H2.equals(targetDB) && databaseProvider.endsWith("." + H2)) {
            System.clearProperty("db.host");
            System.clearProperty("db.user");
            System.clearProperty("db.password");
            System.clearProperty("db.port");
            System.clearProperty("db.name");
            return true;
        }

        return databaseProvider.endsWith("." + targetDB);
    }
}
