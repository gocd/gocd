/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.JobNotFoundException;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.StageDao;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestfulService {
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineService pipelineService;
    @Autowired private StageDao stageDao;
    @Autowired private JobResolverService jobResolverService;

    /**
     * buildId should only be given when caller is absolutely sure about the job instance
     * (makes sense in agent-uploading artifacts/properties scenario because agent won't run a job if its copied over(it only executes real jobs)) -JJ
     */
    public JobIdentifier findJob(String pipelineName, String counterOrLabel, String stageName, String stageCounter, String buildName, Long buildId) {
        JobConfigIdentifier jobConfig = goConfigService.translateToActualCase(new JobConfigIdentifier(pipelineName, stageName, buildName));

        Pipeline pipeline = pipelineService.findPipelineByCounterOrLabel(jobConfig.getPipelineName(), counterOrLabel);

        stageCounter = StringUtils.isEmpty(stageCounter) ? JobIdentifier.LATEST : stageCounter;
        StageIdentifier stageIdentifier = translateStageCounter(pipeline.getIdentifier(), jobConfig.getStageName(), stageCounter);

        JobIdentifier jobId;
        if (buildId == null) {
            jobId = jobResolverService.actualJobIdentifier(new JobIdentifier(stageIdentifier, jobConfig.getJobName()));
        } else {
            jobId = new JobIdentifier(stageIdentifier, jobConfig.getJobName(), buildId);
        }
        if(jobId == null){
            //fix for #5739
            throw new JobNotFoundException(pipelineName, stageName, buildName);
        }
        return jobId;
    }

    public JobIdentifier findJob(String pipelineName, String counterOrLabel, String stageName, String stageCounter, String buildName) {
        return findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName, null);
    }

    public JobIdentifier findJob(JobIdentifier job) {
        return findJob(job.getPipelineName(), job.getPipelineLabel(), job.getStageName(), job.getStageCounter(), job.getBuildName(), job.getBuildId());
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
