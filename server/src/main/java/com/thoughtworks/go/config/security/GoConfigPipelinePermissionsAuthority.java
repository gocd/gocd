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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.permissions.EveryonePermission;
import com.thoughtworks.go.config.security.permissions.PipelinePermission;
import com.thoughtworks.go.config.security.permissions.StagePermission;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.SystemEnvironment.ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP;

/* Understands which users can view, operate and administer which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService, SystemEnvironment systemEnvironment) {
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
    }

    public Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions() {
        return pipelinesInGroupsAndTheirPermissions(goConfigService.groups());
    }

    public Permissions permissionsForPipeline(CaseInsensitiveString pipelineName) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
        return pipelinesInGroupsAndTheirPermissions(new PipelineGroups(group)).get(pipelineName);
    }

    public Permissions permissionsForEmptyGroup(PipelineConfigs group) {
        PipelineGroupsSecurityHelper security = new PipelineGroupsSecurityHelper(goConfigService.security());
        return groupPermissionsOnPipeline(security, group, null);
    }

    private Map<CaseInsensitiveString, Permissions> pipelinesInGroupsAndTheirPermissions(PipelineGroups groups) {
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = new HashMap<>();

        PipelineGroupsSecurityHelper security = new PipelineGroupsSecurityHelper(goConfigService.security());

        groups.accept(group -> {
            for (PipelineConfig pipeline : group) {
                pipelinesAndTheirPermissions.put(pipeline.name(), groupPermissionsOnPipeline(security, group, pipeline));
            }
        });

        return pipelinesAndTheirPermissions;
    }

    private Permissions groupPermissionsOnPipeline(PipelineGroupsSecurityHelper security, PipelineConfigs group, PipelineConfig pipeline) {
        if (security.hasNoSuperAdmins()) {
            return new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE);
        }

        GroupSecurity policy = security.forGroup(group);

        if (!group.hasAuthorizationDefined()) {
            boolean everyoneIsAllowedToViewGroupsWithNoAuth = systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP);
            Users viewersAndOperators = everyoneIsAllowedToViewGroupsWithNoAuth ? Everyone.INSTANCE : policy.effectiveAdmins();;
            return new Permissions(viewersAndOperators, viewersAndOperators, policy.effectiveAdmins(), PipelinePermission.from(pipeline, viewersAndOperators));
        }

        PipelinePermission pipelinePermission = EveryonePermission.INSTANCE;

        if(null != pipeline) {
            pipelinePermission = pipeline.stream().map(stage -> new StagePermission(stage.name().toString(), policy.operatorsForStage(stage))).collect(Collectors.toCollection(PipelinePermission::new));
        }

        return new Permissions(
                policy.effectiveViewers(),
                policy.effectiveOperators(),
                policy.effectiveAdmins(),
                pipelinePermission
        );
    }
}
