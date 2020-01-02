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
package com.thoughtworks.go.apiv3.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;

public class PartialConfigRepresenter {
    private PartialConfigRepresenter() {
    }

    public static void toJSON(OutputWriter json, PartialConfig config) {
        EnvironmentsConfig envs = config.getEnvironments();
        PipelineGroups groups = config.getGroups();

        if (!envs.isEmpty()) {
            json.addChildList("environments", (w) -> envs.forEach(
                    (env) -> w.addChild((ew -> EnvironmentConfigRepresenter.toJSON(ew, env)))
            ));
        }

        if (!groups.isEmpty()) {
            json.addChildList("groups", (w) -> groups.forEach(
                    (group) -> w.addChild(gw -> PipelineGroupRepresenter.toJSON(gw, group))
            ));
        }
    }
}
