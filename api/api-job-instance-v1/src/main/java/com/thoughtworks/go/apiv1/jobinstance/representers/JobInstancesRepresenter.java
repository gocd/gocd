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
package com.thoughtworks.go.apiv1.jobinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.spark.Routes;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class JobInstancesRepresenter {
    public static void toJSON(OutputWriter outputWriter, JobInstances jobInstances, PipelineRunIdInfo runIdInfo) {
        if (jobInstances.isEmpty()) {
            outputWriter.addChildList("jobs", emptyList());
            return;
        }
        addLinks(outputWriter, jobInstances, runIdInfo);
        outputWriter
                .addChildList("jobs", jobsWriter -> jobInstances.forEach(jobInstance -> jobsWriter.addChild(jobWriter -> JobInstanceRepresenter.toJSON(jobWriter, jobInstance))));
    }

    private static void addLinks(OutputWriter outputWriter, JobInstances jobInstances, PipelineRunIdInfo runIdInfo) {
        JobInstance latest = jobInstances.first();
        JobInstance oldest = jobInstances.last();
        String previousLink = (latest.getId() != runIdInfo.getLatestRunId())
                ? Routes.Job.previous(latest.getPipelineName(), latest.getStageName(), latest.getName(), latest.getId())
                : null;
        String nextLink = (oldest.getId() != runIdInfo.getOldestRunId())
                ? Routes.Job.next(oldest.getPipelineName(), oldest.getStageName(), oldest.getName(), oldest.getId())
                : null;
        if (isNotBlank(previousLink) || isNotBlank(nextLink)) {
            outputWriter.addLinks(outputLinkWriter -> {
                outputLinkWriter.addLinkIfPresent("previous", previousLink);
                outputLinkWriter.addLinkIfPresent("next", nextLink);
            });
        }
    }
}
