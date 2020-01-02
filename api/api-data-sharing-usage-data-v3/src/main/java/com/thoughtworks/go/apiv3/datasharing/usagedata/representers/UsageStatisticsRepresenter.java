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
package com.thoughtworks.go.apiv3.datasharing.usagedata.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.domain.UsageStatistics;

public class UsageStatisticsRepresenter {
    private static final int MESSAGE_SCHEMA_VERSION = 3;

    public static void toJSON(OutputWriter outputWriter, UsageStatistics usageStatistics) {
        outputWriter
                .add("server_id", usageStatistics.serverId())
                .add("message_version", MESSAGE_SCHEMA_VERSION)
                .addChild("data", childWriter -> {
                    childWriter
                            .add("pipeline_count", usageStatistics.pipelineCount())
                            .add("config_repo_pipeline_count", usageStatistics.configRepoPipelineCount())
                            .add("agent_count", usageStatistics.agentCount())
                            .add("oldest_pipeline_execution_time", usageStatistics.oldestPipelineExecutionTime())
                            .add("job_count", usageStatistics.jobCount())
                            .addChildList("elastic_agent_job_count", child -> {
                                usageStatistics.elasticAgentPluginToJobCount().forEach((pluginId, jobCount) -> {
                                    child.addChild(c -> {
                                        c.add("plugin_id", pluginId);
                                        c.add("job_count", jobCount);
                                    });
                                });
                            })
                            .add("test_drive_instance", usageStatistics.testDrive())
                            .addChild("test_drive_metrics", testDriveWriter -> {
                               testDriveWriter.add("add_pipeline_cta", usageStatistics.addCTA());
                               testDriveWriter.add("save_and_run_cta", usageStatistics.saveAndRunCTA());
                            })
                            .add("gocd_version", usageStatistics.gocdVersion());
                });
    }

}
