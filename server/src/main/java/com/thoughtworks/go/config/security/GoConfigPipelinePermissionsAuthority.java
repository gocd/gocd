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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/* Understands which users can view, operate and administer which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private GoConfigService goConfigService;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
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
            return new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE);
        }

        GroupSecurity policy = security.forGroup(group);

        if (!group.hasAuthorizationDefined()) {
            return new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, policy.effectiveAdmins(), Everyone.INSTANCE);
        }

        return new Permissions(
                policy.effectiveViewers(),
                policy.effectiveOperators(),
                policy.effectiveAdmins(),
                null == pipeline ? Everyone.INSTANCE : policy.operatorsForPipeline(pipeline)
        );
    }
}
