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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.PluginPreference;

public class SCMPreference implements PluginPreference {
    private SCMConfigurations scmConfigurations;
    private SCMView scmView;

    public SCMPreference(SCMConfigurations scmConfigurations, SCMView scmView) {
        this.scmConfigurations = scmConfigurations;
        this.scmView = scmView;
    }

    public SCMConfigurations getScmConfigurations() {
        return scmConfigurations;
    }

    public SCMView getScmView() {
        return scmView;
    }
}
