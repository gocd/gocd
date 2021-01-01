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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT;
import static com.thoughtworks.go.util.SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT;

public class DiskSpaceSimulator {
    public void onTearDown() {
        new SystemEnvironment().clearProperty(ARTIFACT_FULL_SIZE_LIMIT);
        new SystemEnvironment().clearProperty(ARTIFACT_WARNING_SIZE_LIMIT);
    }

    public String simulateDiskSpaceLow() {
        onTearDown();
        long space = new File(".").getFreeSpace() / 1024;
        String limit = (space-10) + "M";
        new SystemEnvironment().setProperty(ARTIFACT_WARNING_SIZE_LIMIT, limit);
        return limit;
    }

    public String simulateDiskFull() {
        onTearDown();
        String limit = Integer.MAX_VALUE + "M";
        new SystemEnvironment().setProperty(ARTIFACT_FULL_SIZE_LIMIT, limit);
        return limit;
    }
}
