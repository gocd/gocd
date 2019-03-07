/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.agents.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;
import java.util.Map;

public class AgentsRepresenter {

    public static void toJSON(OutputWriter writer, Map<AgentInstance, Collection<EnvironmentConfig>> agentInstanceCollectionMap, SecurityService securityService, Username username) {
        writer.addLinks(
                outputLinkWriter -> outputLinkWriter
                        .addLink("self", Routes.AgentsAPI.BASE)
                        .addAbsoluteLink("doc", Routes.AgentsAPI.DOC))
                .addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("agents",
                        agentsWriter -> agentInstanceCollectionMap.forEach((key, value) -> agentsWriter.addChild(agentWriter -> AgentRepresenter.toJSON(agentWriter, key, value, securityService, username))))
                );
    }
}
