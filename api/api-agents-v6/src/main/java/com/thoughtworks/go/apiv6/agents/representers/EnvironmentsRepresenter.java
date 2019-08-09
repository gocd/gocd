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

package com.thoughtworks.go.apiv6.agents.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class EnvironmentsRepresenter {
    public static void toJSON(OutputListWriter writer, Collection<EnvironmentConfig> environments, String agentUuid) {
        List<EnvironmentConfig> sortedEnvironmentConfigs = environments.stream()
                .sorted(new EnvironmentConfigComparator())
                .collect(Collectors.toList());

        for (EnvironmentConfig environment : sortedEnvironmentConfigs) {
            writer.addChild((childWriter) -> {
                childWriter.add("name", environment.name());
                childWriter.addChild("origin", (originWriter) -> origin(originWriter, environment, agentUuid));
            });
        }
    }

    private static void origin(OutputWriter writer, EnvironmentConfig environment, String agentUuid) {
        if (environment.isUnknown()) {
            writer.add("type", "unknown");
            return;
        }
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

    static class EnvironmentConfigComparator implements Comparator<EnvironmentConfig> {
        @Override
        public int compare(EnvironmentConfig o1, EnvironmentConfig o2) {
            return o1.name().compareTo(o2.name());
        }
    }
}
