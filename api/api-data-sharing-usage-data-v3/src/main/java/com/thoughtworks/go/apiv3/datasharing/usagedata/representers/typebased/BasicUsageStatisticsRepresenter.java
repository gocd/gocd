/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv3.datasharing.usagedata.representers.typebased;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv3.datasharing.usagedata.representers.UsageDataTypeRepresenter;
import com.thoughtworks.go.server.domain.UsageStatistics;

public class BasicUsageStatisticsRepresenter implements UsageDataTypeRepresenter {
    @Override
    public void toJSON(OutputWriter outputWriter, UsageStatistics usageStatistics) {
        outputWriter.add("pipeline_count", usageStatistics.pipelineCount())
                .add("config_repo_pipeline_count", usageStatistics.configRepoPipelineCount())
                .add("agent_count", usageStatistics.agentCount())
                .add("oldest_pipeline_execution_time", usageStatistics.oldestPipelineExecutionTime())
                .add("job_count", usageStatistics.jobCount())
                .add("gocd_version", usageStatistics.gocdVersion())
                .addChildList("elastic_agent_job_count", child -> {
                    usageStatistics.elasticAgentPluginToJobCount().forEach((pluginId, jobCount) -> {
                        child.addChild(c -> {
                            c.add("plugin_id", pluginId);
                            c.add("job_count", jobCount);
                        });
                    });
                });
    }
}
