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
package com.thoughtworks.go.apiv3.environments.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentAgentConfig;
import com.thoughtworks.go.spark.Routes;

public class AgentRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentAgentConfig agentViewModel) {
        outputWriter
                .addLinks(linksWriter -> addLinks(linksWriter, agentViewModel))
                .add("uuid", agentViewModel.getUuid());
    }

    private static void addLinks(OutputLinkWriter linksWriter, EnvironmentAgentConfig agentViewModel) {
        String uuid = agentViewModel.getUuid();
        linksWriter.addLink("self", Routes.AgentsAPI.uuid(uuid))
                .addAbsoluteLink("doc", Routes.AgentsAPI.DOC)
                .addAbsoluteLink("find", Routes.AgentsAPI.find());
    }
}