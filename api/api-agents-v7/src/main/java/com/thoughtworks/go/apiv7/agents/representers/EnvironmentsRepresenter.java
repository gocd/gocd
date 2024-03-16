/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class EnvironmentsRepresenter {
    public static void toJSON(OutputListWriter writer, Collection<EnvironmentConfig> environments, AgentInstance agentInstance) {
        EnvironmentsConfig envConfigs = new EnvironmentsConfig();
        envConfigs.addAll(environments);
        Stream<String> agentEnvAssociationFromDB = agentInstance.getAgent().getEnvironmentsAsList().stream();
        Stream<String> agentEnvAssociationFromConfigRepo = envConfigs.stream()
                .filter(environmentConfig -> !environmentConfig.isLocal())
                .map(environmentConfig -> environmentConfig.name().toString());
        List<String> sortedEnvNames = Stream.concat(agentEnvAssociationFromDB, agentEnvAssociationFromConfigRepo)
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .toList();

        for (String envName : sortedEnvNames) {
            EnvironmentConfig envConfig = envConfigs.find(new CaseInsensitiveString(envName));
            if (envConfig != null) {
                writer.addChild((childWriter) -> {
                    childWriter.add("name", envName);
                    childWriter.addChild("origin", (originWriter) -> origin(originWriter, envConfig, agentInstance.getUuid()));
                });
            } else {
                writer.addChild((childWriter) -> {
                    childWriter.add("name", envName);
                    childWriter.addChild("origin", (originWriter) -> originWriter.add("type", "unknown"));
                });
            }
        }
    }

    private static void origin(OutputWriter writer, EnvironmentConfig environment, String agentUuid) {
        if (environment.containsAgentRemotely(agentUuid)) {
            ConfigOrigin originForAgent = environment.originForAgent(agentUuid)
                    .orElseThrow(() -> bomb(String.format("Did not expect config origin to be null for Environment: %s, Agent: %s",
                            environment.name(), agentUuid)));
            writeConfigRepoOrigin(writer, (RepoConfigOrigin) originForAgent);
        } else {
            writeConfigXmlOrigin(writer);
        }
    }

    private static void writeConfigRepoOrigin(OutputWriter writer, RepoConfigOrigin repoConfigOrigin) {
        writer.add("type", "config-repo");
        writer.addLinks((linksWriter) -> {
            linksWriter.addLink("self", Routes.ConfigRepos.id(repoConfigOrigin.getConfigRepo().getId()));
            linksWriter.addAbsoluteLink("doc", Routes.ConfigRepos.DOC);
            linksWriter.addLink("find", Routes.ConfigRepos.find());
        });
    }

    private static void writeConfigXmlOrigin(OutputWriter writer) {
        writer.add("type", "gocd");
        writer.addLinks((linksWriter) -> {
            linksWriter.addLink("self", Routes.ConfigView.SELF);
            linksWriter.addAbsoluteLink("doc", apiDocsUrl("#get-configuration"));
        });
    }
}
