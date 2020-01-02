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
package com.thoughtworks.go.apiv1.servermaintenancemode.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.shared.representers.materials.MaterialRepresenter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.server.service.MaintenanceModeService.MaterialPerformingMDU;
import com.thoughtworks.go.spark.Routes;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class MaintenanceModeInfoRepresenter {
    public static void toJSON(OutputWriter jsonWriter, ServerMaintenanceMode serverMaintenanceMode, boolean hasNoRunningSystems, Collection<MaterialPerformingMDU> runningMDUs, List<JobInstance> buildingJobs, List<JobInstance> scheduledJobs) {
        jsonWriter
                .addLinks(linksWriter -> linksWriter.addLink("self", Routes.MaintenanceMode.BASE + Routes.MaintenanceMode.INFO)
                        .addAbsoluteLink("doc", Routes.MaintenanceMode.INFO_DOC));
        jsonWriter.add("is_maintenance_mode", serverMaintenanceMode.isMaintenanceMode());
        jsonWriter.addChild("metadata", metadataChildWriter -> {
            metadataChildWriter.add("updated_by", serverMaintenanceMode.updatedBy());
            metadataChildWriter.add("updated_on", serverMaintenanceMode.updatedOn());
        });

        if (serverMaintenanceMode.isMaintenanceMode()) {
            jsonWriter.addChild("attributes", attributesWriter -> {
                attributesWriter.add("has_running_systems", !hasNoRunningSystems);
                attributesWriter.addChild("running_systems", runningSystemsChildWriter -> {
                    runningSystemsChildWriter.addChildList("material_update_in_progress", runningMDUsToJSON(runningMDUs));
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
