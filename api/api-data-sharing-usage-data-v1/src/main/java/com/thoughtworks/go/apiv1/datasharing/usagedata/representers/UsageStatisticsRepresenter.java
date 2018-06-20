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

package com.thoughtworks.go.apiv1.datasharing.usagedata.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.spark.Routes.DataSharing;

public class UsageStatisticsRepresenter {
    public static void toJSON(OutputWriter outputWriter, UsageStatistics usageStatistics, boolean includeLinks) {
        if (includeLinks) {
            outputWriter.addLinks(linksWriter -> linksWriter.addLink("self", DataSharing.USAGE_DATA_PATH));
        }

        outputWriter
                .addChild("_embedded", childWriter -> {
                    childWriter
                            .add("pipeline_count", usageStatistics.pipelineCount())
                            .add("agent_count", usageStatistics.agentCount())
                            .add("oldest_pipeline_execution_time", usageStatistics.oldestPipelineExecutionTime());
                });
    }

}
