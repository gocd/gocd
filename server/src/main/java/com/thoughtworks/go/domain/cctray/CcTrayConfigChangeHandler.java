/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* Understands what needs to be done to keep the CCTray cache updated, when the config changes. */
@Component
public class CcTrayConfigChangeHandler {
    private final CcTrayCache cache;
    private final CcTrayStageStatusLoader stageStatusLoader;
    private final GoConfigPipelinePermissionsAuthority pipelinePermissionsAuthority;

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
        List<ProjectStatus> projectStatuses = new ArrayList<>();
        Users usersWithViewPermissionsOfThisPipeline = pipelinePermissionsAuthority.permissionsForPipeline(pipelineConfig.name()).viewers();
        appendProjectStatusForPipeline(usersWithViewPermissionsOfThisPipeline, pipelineConfig, projectStatuses);
        cache.putAll(projectStatuses);
    }

    private List<ProjectStatus> findAllProjectStatusesForStagesAndJobsIn(CruiseConfig config) {
        final List<ProjectStatus> projectStatuses = new ArrayList<>();
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = pipelinePermissionsAuthority.pipelinesAndTheirPermissions();

        config.accept((PipelineGroupVisitor) group -> {
            for (PipelineConfig pipelineConfig : group) {
                Users usersWithViewPermissionsForPipeline = pipelinesAndTheirPermissions.getOrDefault(pipelineConfig.name(), Permissions.NOONE).viewers();
                appendProjectStatusForPipeline(usersWithViewPermissionsForPipeline, pipelineConfig, projectStatuses);
            }
        });

        return projectStatuses;
    }

    private void appendProjectStatusForPipeline(Users usersWithViewPermissionsOfThisGroup, PipelineConfig pipelineConfig, List<ProjectStatus> projectStatuses) {
        for (StageConfig stageConfig : pipelineConfig) {
            List<ProjectStatus> statuses = allStatusesFor(pipelineConfig, stageConfig);

            statuses.forEach(status -> status.updateViewers(usersWithViewPermissionsOfThisGroup));

            projectStatuses.addAll(statuses);
        }
    }

    private Map<String, ProjectStatus> existingStatusesByName(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        return Optional.ofNullable(cache.get(stageProjectName(pipelineConfig, stageConfig)))
            .map(stageStatus -> Stream.concat(Stream.of(stageStatus), jobStatusesFromCache(pipelineConfig, stageConfig)))
            .orElseGet(() -> stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfig, stageConfig).stream())
            .collect(Collectors.toMap(ProjectStatus::name, s -> s));
    }

    private Stream<ProjectStatus> jobStatusesFromCache(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        return stageConfig.getJobs().stream()
                    .map(jobConfig -> jobProjectName(pipelineConfig, stageConfig, jobConfig))
                    .map(cache::get)
                    .filter(Objects::nonNull);
    }

    private List<ProjectStatus> allStatusesFor(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        Map<String, ProjectStatus> statusesByName = existingStatusesByName(pipelineConfig, stageConfig);

        String stageProjectname = stageProjectName(pipelineConfig, stageConfig);
        return Stream.concat(
                Stream.of(statusesByName.getOrDefault(stageProjectname, new ProjectStatus.NullProjectStatus(stageProjectname))),
                stageConfig.getJobs().stream()
                    .map(jobConfig -> jobProjectName(pipelineConfig, stageConfig, jobConfig))
                    .map(jobProjectname -> statusesByName.getOrDefault(jobProjectname, new ProjectStatus.NullProjectStatus(jobProjectname))))
            .toList();
    }

    private String stageProjectName(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        return String.format("%s :: %s", pipelineConfig.name(), stageConfig.name());
    }

    private String jobProjectName(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig) {
        return String.format("%s :: %s :: %s", pipelineConfig.name(), stageConfig.name(), jobConfig.name());
    }
}
