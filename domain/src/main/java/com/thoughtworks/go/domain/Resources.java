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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Resources extends BaseCollection<Resource> {

    public Resources() {
    }

    public Resources(String commaSeparatedResources) {
        final String[] resourceArray = commaSeparatedResources.split(",");
        for (String resource : resourceArray) {
            final String name = StringUtils.trimToNull(resource);

            if (name == null) {
                continue;
            }

            add(new Resource(name));
        }
    }

    public Resources(ResourceConfigs resourceConfigs) {
        toResources(resourceConfigs);
    }

    public Resources(Resource... resources) {
        super(resources);
    }

    public Resources(List<Resource> resources) {
        super(resources);
    }

    private void toResources(ResourceConfigs resourceConfigs) {
        if (resourceConfigs != null) {
            for (ResourceConfig resourceConfig : resourceConfigs) {
                add(new Resource(resourceConfig));
            }
        }
    }

    public ResourceConfigs toResourceConfigs() {
        final ResourceConfigs resourceConfigs = new ResourceConfigs();
        for (Resource resource : this) {
            resourceConfigs.add(new ResourceConfig(resource.getName()));
        }
        return resourceConfigs;
    }
}
