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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.permissions.PipelinePermission;
import com.thoughtworks.go.config.security.permissions.StageDerivedPipelinePermission;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.GoConfigService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/* Understands which users can view, operate and administer which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private final GoConfigService goConfigService;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions() {
        return pipelinesInGroupsAndTheirPermissions(goConfigService.groups());
    }

    public @NotNull Permissions permissionsForPipeline(CaseInsensitiveString pipelineName) {
        return goConfigService.findGroupByPipelineOptional(pipelineName)
            .map(group -> pipelinesInGroupsAndTheirPermissions(new PipelineGroups(group)).get(pipelineName))
            .orElse(Permissions.NOONE);
    }

    public Permissions permissionsForEmptyGroup(PipelineConfigs group) {
        PipelineGroupsSecurityHelper security = new PipelineGroupsSecurityHelper(goConfigService.security());
        return groupPermissionsOnPipeline(security, group, Optional.empty());
    }

    private Map<CaseInsensitiveString, Permissions> pipelinesInGroupsAndTheirPermissions(PipelineGroups groups) {
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = new HashMap<>();

        PipelineGroupsSecurityHelper security = new PipelineGroupsSecurityHelper(goConfigService.security());

        groups.accept(group -> {
            for (PipelineConfig pipeline : group) {
                pipelinesAndTheirPermissions.put(pipeline.name(), groupPermissionsOnPipeline(security, group, Optional.of(pipeline)));
            }
        });

        return pipelinesAndTheirPermissions;
    }

    private Permissions groupPermissionsOnPipeline(PipelineGroupsSecurityHelper security, PipelineConfigs group, Optional<PipelineConfig> pipeline) {
        if (security.hasNoSuperAdmins()) {
            return Permissions.EVERYONE;
        }

        GroupSecurity policy = security.forGroup(group);

        if (!group.hasAuthorizationDefined()) {
            return new Permissions(
                policy.effectiveAdmins(),
                policy.effectiveAdmins(),
                policy.effectiveAdmins(),
                pipeline.map(p -> StageDerivedPipelinePermission.from(p, ignore -> policy.effectiveAdmins())).orElse(PipelinePermission.EVERYONE)
            );
        }

        return new Permissions(
            policy.effectiveViewers(),
            policy.effectiveOperators(),
            policy.effectiveAdmins(),
            pipeline.map(p -> StageDerivedPipelinePermission.from(p, policy::operatorsForStage)).orElse(PipelinePermission.EVERYONE)
        );
    }

}
