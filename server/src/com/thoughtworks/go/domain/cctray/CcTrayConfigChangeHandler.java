/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/* Understands what needs to be done to keep the CCTray cache updated, when the config changes. */
@Component
public class CcTrayConfigChangeHandler {
    private CcTrayCache cache;
    private CcTrayStageStatusLoader stageStatusLoader;

    @Autowired
    public CcTrayConfigChangeHandler(CcTrayCache cache, CcTrayStageStatusLoader stageStatusLoader) {
        this.cache = cache;
        this.stageStatusLoader = stageStatusLoader;
    }

    public void call(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(findAllProjectStatusesForStagesAndJobsIn(config));
    }

    private List<ProjectStatus> findAllProjectStatusesForStagesAndJobsIn(CruiseConfig config) {
        final List<ProjectStatus> projectStatuses = new ArrayList<ProjectStatus>();

        config.accept(new PiplineConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig) {
                for (StageConfig stageConfig : pipelineConfig) {

                    String stageProjectName = stageProjectName(pipelineConfig, stageConfig);
                    if (cache.get(stageProjectName) != null) {
                        List<ProjectStatus> statusesInCache = findStageAndStatusesFromCache(pipelineConfig, stageConfig);
                        addAllWithDefaultsForMissingStatuses(projectStatuses, pipelineConfig, stageConfig, statusesInCache);
                    } else {
                        List<ProjectStatus> statusesInDB = findStageAndStatusesFromDB(pipelineConfig, stageConfig);
                        addAllWithDefaultsForMissingStatuses(projectStatuses, pipelineConfig, stageConfig, statusesInDB);
                    }
                }
            }
        });

        return projectStatuses;
    }

    private List<ProjectStatus> findStageAndStatusesFromCache(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        List<ProjectStatus> projectStatuses = new ArrayList<ProjectStatus>();

        String stageProjectName = stageProjectName(pipelineConfig, stageConfig);
        projectStatuses.add(cache.get(stageProjectName));

        for (JobConfig jobConfig : stageConfig.getJobs()) {
            ProjectStatus jobStatus = cache.get(jobProjectName(stageProjectName, jobConfig));
            if (jobStatus != null) {
                projectStatuses.add(jobStatus);
            }
        }
        return projectStatuses;
    }

    private List<ProjectStatus> findStageAndStatusesFromDB(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        return stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfig, stageConfig);
    }

    private void addAllWithDefaultsForMissingStatuses(List<ProjectStatus> allStatuses,
                                                      PipelineConfig pipelineConfig, StageConfig stageConfig,
                                                      List<ProjectStatus> statusesAvailableForThisStage) {
        String stageProjectName = stageProjectName(pipelineConfig, stageConfig);
        allStatuses.add(findOrDefault(stageProjectName, statusesAvailableForThisStage));

        for (JobConfig jobConfig : stageConfig.getJobs()) {
            String jobProjectName = jobProjectName(stageProjectName, jobConfig);
            allStatuses.add(findOrDefault(jobProjectName, statusesAvailableForThisStage));
        }
    }

    private ProjectStatus findOrDefault(String projectName, List<ProjectStatus> statusesToSearchIn) {
        for (ProjectStatus status : statusesToSearchIn) {
            if (status.name().equals(projectName)) {
                return status;
            }
        }
        return new ProjectStatus.NullProjectStatus(projectName);
    }

    private String stageProjectName(final PipelineConfig pipelineConfig, final StageConfig stageConfig) {
        return String.format("%s :: %s", pipelineConfig.name().toString(), stageConfig.name().toString());
    }

    private String jobProjectName(String stageProjectName, JobConfig jobConfig) {
        return String.format("%s :: %s", stageProjectName, jobConfig.name().toString());
    }
}
