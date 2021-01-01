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
package com.thoughtworks.go.apiv7.agents.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.Routes;

import java.util.*;
import java.util.stream.Collectors;

public class AgentRepresenter {
    public static void toJSON(OutputWriter outputWriter, AgentInstance agentInstance, Collection<EnvironmentConfig> environments, SecurityService securityService, Username username) {
        outputWriter
                .addLinks(linksWriter -> linksWriter
                        .addLink("self", Routes.AgentsAPI.uuid(agentInstance.getUuid()))
                        .addAbsoluteLink("doc", Routes.AgentsAPI.DOC)
                        .addLink("find", Routes.AgentsAPI.find()))
                .add("uuid", agentInstance.getUuid())
                .add("hostname", agentInstance.getHostname())
                .add("ip_address", agentInstance.getIpAddress())
                .add("sandbox", agentInstance.getLocation())
                .add("operating_system", agentInstance.getOperatingSystem())
                .add("agent_config_state", agentInstance.getAgentConfigStatus().toString())
                .add("agent_state", agentInstance.getRuntimeStatus().agentState().toString())
                .add("agent_version", agentInstance.getAgentVersion())
                .add("agent_bootstrapper_version", agentInstance.getAgentBootstrapperVersion())
                .addChildList("environments", envWriter -> EnvironmentsRepresenter.toJSON(envWriter, environments, agentInstance))
                .add("build_state", agentInstance.getRuntimeStatus().buildState().toString());

        if (agentInstance.freeDiskSpace() == null || agentInstance.freeDiskSpace().isNullDiskspace()) {
            outputWriter.add("free_space", "unknown");
        } else {
            outputWriter.add("free_space", agentInstance.freeDiskSpace().space());
        }

        if (isBuilding(agentInstance) && hasViewOrOperatePermissionOnPipeline(agentInstance, securityService, username)) {
            outputWriter
                    .addChild("build_details", buildDetailsWriter -> BuildDetailsRepresenter.toJSON(buildDetailsWriter, agentInstance.getBuildingInfo()));
        }

        if (agentInstance.isElastic()) {
            outputWriter
                    .add("elastic_plugin_id", agentInstance.elasticAgentMetadata().elasticPluginId())
                    .add("elastic_agent_id", agentInstance.elasticAgentMetadata().elasticAgentId());
        } else {
            outputWriter.addChildList("resources", sortedResources(agentInstance));
        }

        if (!agentInstance.errors().isEmpty()) {
            Map<String, String> fieldMapping = new HashMap<String, String>() {{
                put("ipAddress", "ip_address");
                put("elasticAgentId", "elastic_agent_id");
                put("elasticPluginId", "elastic_plugin_id");
            }};
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, agentInstance.errors()));
        }
    }

    private static boolean isBuilding(AgentInstance agentInstance) {
        return agentInstance.getBuildingInfo().isBuilding();
    }

    private static boolean hasViewOrOperatePermissionOnPipeline(AgentInstance agentInstance, SecurityService securityService, Username username) {
        return securityService.hasViewOrOperatePermissionForPipeline(username, agentInstance.getBuildingInfo().getPipelineName());
    }

    private static List<String> sortedResources(AgentInstance agentInstance) {
        if (agentInstance.isElastic()) {
            return Collections.emptyList();
        }

        return agentInstance.getResourceConfigs().resourceNames().stream().sorted().collect(Collectors.toList());
    }
}
