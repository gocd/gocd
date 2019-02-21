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

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentsRepresenter {
    public static void toJSON(OutputListWriter writer, Collection<EnvironmentConfig> environments, String agentUuid) {
        List<EnvironmentConfig> sortedEnvironmentConfigs = environments.stream()
                .sorted(new EnvironmentConfigComparator())
                .collect(Collectors.toList());

        for (EnvironmentConfig environment : sortedEnvironmentConfigs) {
            writer.addChild((childWriter) -> {
                childWriter.add("name", environment.name());
                childWriter.add("associated_from_config_repo", environment.containsAgentRemotely(agentUuid));
            });
        }
    }

    static class EnvironmentConfigComparator implements Comparator<EnvironmentConfig> {
        @Override
        public int compare(EnvironmentConfig o1, EnvironmentConfig o2) {
            return o1.name().compareTo(o2.name());
        }
    }
}
