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
package com.thoughtworks.go.plugin.infra;

import org.osgi.framework.Constants;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

class ServiceQuery {
    private final List<String> criterias = new ArrayList<>();

    public static ServiceQuery newQuery(String pluginId) {
        return new ServiceQuery().withPluginId(pluginId);
    }

    public String build() {
        return format("(&%s)", String.join("", criterias));
    }

    public ServiceQuery withExtension(String extensionType) {
        return addCriteria(Constants.BUNDLE_CATEGORY, extensionType);
    }

    public ServiceQuery withPluginId(String pluginId) {
        return addCriteria("PLUGIN_ID", pluginId);
    }

    private ServiceQuery addCriteria(String key, String value) {
        this.criterias.add(format("(%s=%s)", key, value));
        return this;
    }
}
