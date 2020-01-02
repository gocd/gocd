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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.StageDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestfulService {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private JobResolverService jobResolverService;

    /**
     * buildId should only be given when caller is absolutely sure about the job instance
     * (makes sense in agent-uploading artifacts/properties scenario because agent won't run a job if its copied over(it only executes real jobs)) -JJ
     * <p>
     * This does not return pipelineLabel
     */
    public JobIdentifier findJob(String pipelineName, String pipelineCounter, String stageName, String stageCounter, String buildName, Long buildId) {
        JobConfigIdentifier jobConfigIdentifier = goConfigService.translateToActualCase(new JobConfigIdentifier(pipelineName, stageName, buildName));

        PipelineIdentifier pipelineIdentifier;

        if (JobIdentifier.LATEST.equalsIgnoreCase(pipelineCounter)) {
            pipelineIdentifier = pipelineService.mostRecentPipelineIdentifier(jobConfigIdentifier.getPipelineName());
        } else if (StringUtils.isNumeric(pipelineCounter)) {
            pipelineIdentifier = pipelineService.findPipelineByNameAndCounter(pipelineName, Integer.parseInt(pipelineCounter)).getIdentifier();
        } else {
            throw new RuntimeException("Expected numeric pipeline counter but received '%s'" + pipelineCounter);
        }

        stageCounter = StringUtils.isEmpty(stageCounter) ? JobIdentifier.LATEST : stageCounter;
        StageIdentifier stageIdentifier = translateStageCounter(pipelineIdentifier, jobConfigIdentifier.getStageName(), stageCounter);

        JobIdentifier jobId;
        if (buildId == null) {
            jobId = jobResolverService.actualJobIdentifier(new JobIdentifier(stageIdentifier, jobConfigIdentifier.getJobName()));
        } else {
            jobId = new JobIdentifier(stageIdentifier, jobConfigIdentifier.getJobName(), buildId);
        }
        if (jobId == null) {
            //fix for #5739
            throw new RecordNotFoundException(String.format("Job '%s' not found in pipeline '%s' stage '%s'", buildName, pipelineName, stageName));
        }
        return jobId;
    }

    public JobIdentifier findJob(String pipelineName, String pipelineCounter, String stageName, String stageCounter, String buildName) {
        return findJob(pipelineName, pipelineCounter, stageName, stageCounter, buildName, null);
    }

    public StageIdentifier translateStageCounter(PipelineIdentifier pipelineIdentifier, String stageName, String stageCounter) {
        if (JobIdentifier.LATEST.equalsIgnoreCase(stageCounter)) {
            int latestCounter = stageDao.findLatestStageCounter(pipelineIdentifier, stageName);
            return new StageIdentifier(pipelineIdentifier, stageName, String.valueOf(latestCounter));
        } else {
            return new StageIdentifier(pipelineIdentifier, stageName, stageCounter);
        }
    }
}
