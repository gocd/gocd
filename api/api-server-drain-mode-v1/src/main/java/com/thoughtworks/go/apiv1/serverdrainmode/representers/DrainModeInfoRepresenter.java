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

package com.thoughtworks.go.apiv1.serverdrainmode.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.shared.representers.materials.MaterialRepresenter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.server.service.DrainModeService.MaterialPerformingMDU;
import com.thoughtworks.go.spark.Routes;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class DrainModeInfoRepresenter {
    public static void toJSON(OutputWriter jsonWriter, ServerDrainMode serverDrainMode, boolean isServerCompletelyDrained, Collection<MaterialPerformingMDU> runningMDUs, List<JobInstance> buildingJobs, List<JobInstance> scheduledJobs) {
        jsonWriter
                .addLinks(linksWriter -> linksWriter.addLink("self", Routes.DrainMode.BASE + Routes.DrainMode.INFO)
                        .addAbsoluteLink("doc", Routes.DrainMode.INFO_DOC));
        jsonWriter.add("is_drain_mode", serverDrainMode.isDrainMode());
        jsonWriter.addChild("metadata", metadataChildWriter -> {
            metadataChildWriter.add("updated_by", serverDrainMode.updatedBy());
            metadataChildWriter.add("updated_on", serverDrainMode.updatedOn());
        });

        if (serverDrainMode.isDrainMode()) {
            jsonWriter.addChild("attributes", attributesWriter -> {
                attributesWriter.add("is_completely_drained", isServerCompletelyDrained);
                attributesWriter.addChild("running_systems", runningSystemsChildWriter -> {
                    runningSystemsChildWriter.addChildList("mdu", runningMDUsToJSON(runningMDUs));
                    runningSystemsChildWriter.addChildList("building_jobs", runningJobsToJSON(buildingJobs));
                    runningSystemsChildWriter.addChildList("scheduled_jobs", runningJobsToJSON(scheduledJobs));
                });
            });
        }
    }

    private static Consumer<OutputListWriter> runningMDUsToJSON(Collection<MaterialPerformingMDU> runningMDUs) {
        return listWriter -> {
            runningMDUs.stream().forEach(materialPerformingMDU -> {
                listWriter.addChild(childItemWriter -> {
                    MaterialRepresenter.toJSON(childItemWriter, materialPerformingMDU.getMaterial().config());
                    childItemWriter.add("mdu_start_time", materialPerformingMDU.getTimestamp());
                });
            });
        };
    }

    private static Consumer<OutputListWriter> runningJobsToJSON(List<JobInstance> runningJobs) {
        return listWriter -> {
            runningJobs.stream().forEach(job -> {
                listWriter.addChild(childItemWriter -> {
                    childItemWriter.add("pipeline_name", job.getPipelineName());
                    childItemWriter.add("pipeline_counter", job.getPipelineCounter());
                    childItemWriter.add("stage_name", job.getStageName());
                    childItemWriter.add("stage_counter", job.getStageCounter());
                    childItemWriter.add("name", job.getName());
                    childItemWriter.add("state", job.getState().toString());
                    childItemWriter.add("scheduled_date", new Timestamp(job.getScheduledDate().getTime()));
                    childItemWriter.add("agent_uuid", job.getAgentUuid());
                });
            });
        };
    }
}
