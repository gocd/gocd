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

import java.util.Map;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstanceLog;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.LogFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobDetailService {
    private JobInstanceDao jobInstanceDao;
    private ArtifactsService artifactsService;
    private GoConfigService configService;

    public JobDetailService(JobInstanceDao jobInstanceDao) {
        this.jobInstanceDao = jobInstanceDao;
    }

    @Autowired JobDetailService(ArtifactsService artifactsService, JobInstanceDao jobInstanceDao,
                                GoConfigService configService) {
        this.artifactsService = artifactsService;
        this.jobInstanceDao = jobInstanceDao;
        this.configService = configService;
    }

    public void loadBuildInstanceLog(JobInstance instance) throws Exception {
        if (instance.isCompleted()) {
            LogFile logFile = artifactsService.getInstanceLogFile(instance.getIdentifier());
            Map properties = artifactsService.parseLogFile(logFile, instance.isPassed());
            properties.put("artifactfolder", artifactsService.findArtifact(instance.getIdentifier(), ""));
            instance.setInstanceLog(new JobInstanceLog(logFile, properties));
        }
    }

    public JobInstance findMostRecentBuild(JobIdentifier jobIdentifier) throws Exception {
        JobInstance instance = jobInstanceDao.mostRecentJobWithTransitions(jobIdentifier);
        loadBuildInstanceLog(instance);
        return instance;
    }

}
