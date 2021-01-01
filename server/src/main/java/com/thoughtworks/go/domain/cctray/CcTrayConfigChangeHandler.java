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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.GoConfigPipelinePermissionsAuthority;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.config.security.users.Users;
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
    private GoConfigPipelinePermissionsAuthority pipelinePermissionsAuthority;

    @Autowired
    public CcTrayConfigChangeHandler(CcTrayCache cache, CcTrayStageStatusLoader stageStatusLoader, GoConfigPipelinePermissionsAuthority pipelinePermissionsAuthority) {
        this.cache = cache;
        this.stageStatusLoader = stageStatusLoader;
        this.pipelinePermissionsAuthority = pipelinePermissionsAuthority;
    }

    public void call(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(findAllProjectStatusesForStagesAndJobsIn(config));
    }

    public void call(PipelineConfig pipelineConfig) {
        ArrayList<ProjectStatus> projectStatuses = new ArrayList<>();
        final Permissions permissions = pipelinePermissionsAuthority.permissionsForPipeline(pipelineConfig.name());
        Users usersWithViewPermissionsOfThisPipeline = viewersOrNoOne(permissions);
        updateProjectStatusForPipeline(usersWithViewPermissionsOfThisPipeline, pipelineConfig, projectStatuses);
        cache.putAll(projectStatuses);
    }

    private List<ProjectStatus> findAllProjectStatusesForStagesAndJobsIn(CruiseConfig config) {
        final List<ProjectStatus> projectStatuses = new ArrayList<>();
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = pipelinePermissionsAuthority.pipelinesAndTheirPermissions();

        config.accept((PipelineGroupVisitor) group -> {
            for (PipelineConfig pipelineConfig : group) {
                Users usersWithViewPermissionsForPipeline = usersWithViewPermissionsFor(pipelineConfig, pipelinesAndTheirPermissions);
                updateProjectStatusForPipeline(usersWithViewPermissionsForPipeline, pipelineConfig, projectStatuses);
            }
        });

        return projectStatuses;
    }

    private void updateProjectStatusForPipeline(Users usersWithViewPermissionsOfThisGroup, PipelineConfig pipelineConfig, List<ProjectStatus> projectStatuses) {
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

    private void updateStatusesWithUsersHavingViewPermission(List<ProjectStatus> statuses, Users viewersOfThisGroup) {
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

    private Users usersWithViewPermissionsFor(PipelineConfig pipelineConfig, Map<CaseInsensitiveString, Permissions> allPipelinesAndTheirPermissions) {
        Permissions permissions = allPipelinesAndTheirPermissions.get(pipelineConfig.name());
        return viewersOrNoOne(permissions);
    }

    private Users viewersOrNoOne(Permissions permissions) {
        return permissions == null ? NoOne.INSTANCE : permissions.viewers();
    }
}
