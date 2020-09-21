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
package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;

public class SCMMother {
    public static SCM create(String id) {
        SCM scm = create(id, "scm-" + id, "plugin", "1.0", new Configuration());
        return scm;
    }

    public static SCM create(String id, String name, String pluginId, String pluginVersion, Configuration configuration) {
        SCM scm = new SCM();
        scm.setId(id);
        scm.setName(name);
        scm.setPluginConfiguration(new PluginConfiguration(pluginId, pluginVersion));
        scm.setConfiguration(configuration);
        return scm;
    }
}
