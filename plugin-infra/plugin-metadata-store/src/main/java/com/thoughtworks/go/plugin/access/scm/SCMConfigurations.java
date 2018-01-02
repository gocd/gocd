/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SCMConfigurations {
    private List<SCMConfiguration> configurations = new ArrayList<>();

    public SCMConfigurations() {
    }

    public SCMConfigurations(SCMPropertyConfiguration configuration) {
        for (Property property : configuration.list()) {
            configurations.add(new SCMConfiguration(property));
        }
    }

    public void add(SCMConfiguration configuration) {
        configurations.add(configuration);
    }

    public SCMConfiguration get(String key) {
        for (SCMConfiguration configuration : configurations) {
            if (configuration.getKey().equals(key)) {
                return configuration;
            }
        }
        return null;
    }

    public int size() {
        return configurations.size();
    }

    public List<SCMConfiguration> list() {
        Collections.sort(configurations);
        return configurations;
    }
}
