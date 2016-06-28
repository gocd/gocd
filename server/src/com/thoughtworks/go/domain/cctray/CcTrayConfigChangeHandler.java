/*
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
 */

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.viewers.Viewers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Understands what needs to be done to keep the CCTray cache updated, when the config changes. */
@Component
public class CcTrayConfigChangeHandler {
    private CcTrayCache cache;
    private CcTrayStageStatusLoader stageStatusLoader;
    private CcTrayViewAuthority ccTrayViewAuthority;

    @Autowired
    public CcTrayConfigChangeHandler(CcTrayCache cache, CcTrayStageStatusLoader stageStatusLoader, CcTrayViewAuthority ccTrayViewAuthority) {
        this.cache = cache;
        this.stageStatusLoader = stageStatusLoader;
        this.ccTrayViewAuthority = ccTrayViewAuthority;
    }

    public void call(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(findAllProjectStatusesForStagesAndJobsIn(config));
    }

    public void call(PipelineConfig pipelineConfig, String pipelineGroup) {
        ArrayList<ProjectStatus> projectStatuses = new ArrayList<>();
        final Map<String, Viewers> groupsAndTheirViewers = ccTrayViewAuthority.groupsAndTheirViewers();
        Viewers usersWithViewPermissionsOfThisGroup = groupsAndTheirViewers.get(pipelineGroup);
        updateProjectStatusForPipeline(usersWithViewPermissionsOfThisGroup, pipelineConfig, projectStatuses);
        cache.putAll(projectStatuses);
    }

    private List<ProjectStatus> findAllProjectStatusesForStagesAndJobsIn(CruiseConfig config) {
        final List<ProjectStatus> projectStatuses = new ArrayList<>();
        final Map<String, Viewers> groupsAndTheirViewers = ccTrayViewAuthority.groupsAndTheirViewers();

        config.accept(new PipelineGroupVisitor() {
            @Override
            public void visit(PipelineConfigs pipelineConfigs) {
                Viewers usersWithViewPermissionsOfThisGroup = groupsAndTheirViewers.get(pipelineConfigs.getGroup());

                for (PipelineConfig pipelineConfig : pipelineConfigs) {
                    updateProjectStatusForPipeline(usersWithViewPermissionsOfThisGroup, pipelineConfig, projectStatuses);
                }
            }

        });

        return projectStatuses;
    }

    private void updateProjectStatusForPipeline(Viewers usersWithViewPermissionsOfThisGroup, PipelineConfig pipelineConfig, List<ProjectStatus> projectStatuses) {
        for (StageConfig stageConfig : pipelineConfig) {
            List<ProjectStatus> statusesInCacheOrDB = findExistingStatuses(pipelineConfig, stageConfig);
            List<ProjectStatus> statuses = getStatusesForCurrentProjectsWithDefaultsForMissingOnes(pipelineConfig, stageConfig, statusesInCacheOrDB);
            updateStatusesWithUsersHavingViewPermission(statuses, usersWithViewPermissionsOfThisGroup);

            projectStatuses.addAll(statuses);
        }
    }

    private List<ProjectStatus> findExistingStatuses(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        if (cache.get(stageProjectName(pipelineConfig, stageConfig)) != null) {
            return findStageAndStatusesFromCache(pipelineConfig, stageConfig);
        } else {
            return findStageAndStatusesFromDB(pipelineConfig, stageConfig);
        }
    }

    private List<ProjectStatus> findStageAndStatusesFromCache(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        List<ProjectStatus> projectStatuses = new ArrayList<>();

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

    private List<ProjectStatus> getStatusesForCurrentProjectsWithDefaultsForMissingOnes(PipelineConfig pipelineConfig, StageConfig stageConfig,
                                                                                        List<ProjectStatus> statusesAvailableForThisStage) {
        List<ProjectStatus> allStatuses = new ArrayList<>();

        String stageProjectName = stageProjectName(pipelineConfig, stageConfig);
        allStatuses.add(findOrDefault(stageProjectName, statusesAvailableForThisStage));

        for (JobConfig jobConfig : stageConfig.getJobs()) {
            String jobProjectName = jobProjectName(stageProjectName, jobConfig);
            allStatuses.add(findOrDefault(jobProjectName, statusesAvailableForThisStage));
        }

        return allStatuses;
    }

    private ProjectStatus findOrDefault(String projectName, List<ProjectStatus> statusesToSearchIn) {
        for (ProjectStatus status : statusesToSearchIn) {
            if (status.name().equals(projectName)) {
                return status;
            }
        }
        return new ProjectStatus.NullProjectStatus(projectName);
    }

    private void updateStatusesWithUsersHavingViewPermission(List<ProjectStatus> statuses, Viewers viewersOfThisGroup) {
        for (ProjectStatus status : statuses) {
            status.updateViewers(viewersOfThisGroup);
        }
    }

    private String stageProjectName(final PipelineConfig pipelineConfig, final StageConfig stageConfig) {
        return String.format("%s :: %s", pipelineConfig.name().toString(), stageConfig.name().toString());
    }

    private String jobProjectName(String stageProjectName, JobConfig jobConfig) {
        return String.format("%s :: %s", stageProjectName, jobConfig.name().toString());
    }
}
