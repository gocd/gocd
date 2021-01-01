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
package com.thoughtworks.go.fixture;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;

public class ArtifactsDiskIsFull implements PreCondition {
    @Override
    public void onSetUp() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "10000000m");
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT, "11222334m");
    }

    @Override
    public void onTearDown() throws Exception {
        new SystemEnvironment().clearProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT);
        new SystemEnvironment().clearProperty(SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT);
    }

    public long getLowLimit() {
        return 10000000 * GoConstants.MEGA_BYTE;
    }

}
