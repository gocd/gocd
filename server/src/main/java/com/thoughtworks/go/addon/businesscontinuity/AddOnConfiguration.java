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

package com.thoughtworks.go.addon.businesscontinuity;

import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class AddOnConfiguration {

    @Autowired
    public AddOnConfiguration(SystemEnvironment systemEnvironment) {
        if (isServerInStandby()) {
            bombIf(isEmpty(System.getProperty("bc.primary.url")), "Please provide 'bc.primary.url' system property to start server in standby mode");
            systemEnvironment.set(SystemEnvironment.GO_SERVER_STATE, "passive");
            systemEnvironment.set(SystemEnvironment.GO_LANDING_PAGE, "/add-on/business-continuity/admin/dashboard");
        }
    }

    public boolean isServerInStandby() {
        return "standby".equalsIgnoreCase(System.getProperty("go.server.mode"));
    }
}
