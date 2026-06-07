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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.server.domain.Username;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;

@Service
public class SecurityService {
    private final GoConfigService goConfigService;

    @Autowired
    public SecurityService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean hasViewPermissionForPipeline(Username username, String pipelineName) {
        return goConfigService.findGroupNameByPipelineOptional(cis(pipelineName))
            .map(groupName -> hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), groupName))
            .orElse(true); // TODO change this insecure default?
    }

    public boolean hasViewPermissionForGroup(String userName, String pipelineGroupName) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();

        if (!cruiseConfig.isSecurityEnabled()) {
            return true;
        }

        CaseInsensitiveString username = cis(userName);
        if (isUserAdmin(new Username(username))) {
            return true;
        }

        PipelineConfigs group = cruiseConfig.getGroups().findGroup(pipelineGroupName);
        return isUserAdminOfGroup(username, group) || group.hasViewPermission(username, new UserRoleMatcherImpl(cruiseConfig.server().security()));
    }

    private boolean isUserAdminOfGroup(final CaseInsensitiveString userName, PipelineConfigs group) {
        return goConfigService.isUserAdminOfGroup(userName, group);
    }

    public boolean isUserAdminOfGroup(final CaseInsensitiveString userName, String groupName) {
        return goConfigService.isUserAdminOfGroup(userName, groupName);
    }

    public boolean isUserAdminOfGroup(final Username username, String groupName) {
        return goConfigService.isUserAdminOfGroup(username.getUsername(), groupName);
    }

    public boolean hasOperatePermissionForPipeline(final CaseInsensitiveString username, String pipelineName) {
        return goConfigService.findGroupNameByPipelineOptional(cis(pipelineName))
            .map(groupName -> hasOperatePermissionForGroup(username, groupName))
            .orElse(true); // TODO change this insecure default?
    }

    public boolean hasAdminPermissionsForPipeline(Username username, CaseInsensitiveString pipelineName) {
        return goConfigService.findGroupNameByPipelineOptional(pipelineName)
            .map(groupName -> isUserAdminOfGroup(username, groupName))
            .orElse(true); // TODO change this insecure default?
    }

    public boolean hasOperatePermissionForGroup(final CaseInsensitiveString username, String groupName) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();

        if (!cruiseConfig.isSecurityEnabled()) {
            return true;
        }

        if (isUserAdmin(new Username(username))) {
            return true;
        }

        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        return isUserAdminOfGroup(username, group) || group.hasOperatePermission(username, new UserRoleMatcherImpl(cruiseConfig.server().security()));
    }

    public boolean hasOperatePermissionForStage(String pipelineName, String stageName, String username) {
        if (!goConfigService.isSecurityEnabled()) {
            return true;
        }
        if (!goConfigService.hasStageConfigNamed(pipelineName, stageName)) {
            return false;
        }
        StageConfig stage = goConfigService.stageConfigNamed(pipelineName, stageName);
        CaseInsensitiveString userName = cis(username);

        if (stage.hasOperatePermissionDefined()) {
            PipelineConfigs group = goConfigService.findGroupByPipeline(cis(pipelineName));
            if (isUserAdmin(new Username(userName)) || isUserAdminOfGroup(userName, group)) {
                return true;
            }
            return goConfigService.readAclBy(pipelineName, stageName).isGranted(userName);
        }

        return hasOperatePermissionForPipeline(cis(username), pipelineName);
    }

    public boolean isUserAdmin(Username username) {
        if (!isSecurityEnabled()) {
            return true;
        }
        return goConfigService.isUserAdmin(username);
    }

    public boolean isSecurityEnabled() {
        return goConfigService.isSecurityEnabled();
    }

    public boolean canViewSomeAdminPage(Username username) {
        return canEditSomeAdminPage(username) || isAuthorizedToViewTemplates(username);
    }

    public boolean canEditSomeAdminPage(Username username) {
        return isUserAdmin(username) || isUserGroupAdmin(username) || isAuthorizedToEditTemplates(username);
    }

    public boolean canCreatePipelines(Username username) {
        return isUserAdmin(username) || isUserGroupAdmin(username);
    }

    public boolean hasOperatePermissionForFirstStage(String pipelineName, String userName) {
        StageConfig stage = goConfigService.findFirstStageOfPipeline(cis(pipelineName));
        return hasOperatePermissionForStage(pipelineName, CaseInsensitiveString.str(stage.name()), userName);
    }

    public boolean hasOperatePermissionForAgents(Username username) {
        return isUserAdmin(username);
    }

    public boolean hasViewOrOperatePermissionForPipeline(Username username, String pipelineName) {
        return hasViewPermissionForPipeline(username, pipelineName) ||
                hasOperatePermissionForPipeline(username.getUsername(), pipelineName);
    }

    public List<CaseInsensitiveString> viewablePipelinesFor(Username username) {
        List<CaseInsensitiveString> pipelines = new ArrayList<>();
        for (String group : goConfigService.allGroupNames()) {
            if (hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), group)) {
                pipelines.addAll(goConfigService.pipelines(group));
            }
        }
        return pipelines;
    }

    public boolean isUserGroupAdmin(Username username) {
        return goConfigService.isGroupAdministrator(username.getUsername());
    }

    public List<String> modifiableGroupsForUser(Username userName) {
        if (isUserAdmin(userName)) {
            return goConfigService.allGroupNames();
        }
        List<String> modifiableGroups = new ArrayList<>();
        for (String group : goConfigService.allGroupNames()) {
            if (isUserAdminOfGroup(userName.getUsername(), group)) {
                modifiableGroups.add(group);
            }
        }
        return modifiableGroups;
    }

    public boolean isAuthorizedToEditTemplates(Username username) {
        return goConfigService.cruiseConfig().isAuthorizedToEditTemplates(username.getUsername());
    }

    public boolean isAuthorizedToEditTemplate(CaseInsensitiveString templateName, Username username) {
        return goConfigService.cruiseConfig().isAuthorizedToEditTemplate(templateName, username.getUsername());
    }

    public boolean isAuthorizedToViewTemplate(CaseInsensitiveString templateName, Username username) {
        return goConfigService.cruiseConfig().isAuthorizedToViewTemplate(templateName, username.getUsername());
    }

    public boolean isAuthorizedToViewTemplates(Username username) {
        return goConfigService.cruiseConfig().isAuthorizedToViewTemplates(username.getUsername());
    }

    public boolean doesUserHasPermissions(Username username, SupportedAction action, SupportedEntity entity, String resource, String resourceToOperateWithin) {
        if (isUserAdmin(username)) {
            return true;
        }

        List<Role> roles = goConfigService.rolesForUser(username.getUsername());

        boolean hasPermission = false;
        for (Role role : roles) {
            if (role.hasExplicitDenyPermissionsFor(action, entity.getEntityType(), resource, resourceToOperateWithin)) {
                return false;
            }

            if (role.hasPermissionsFor(action, entity.getEntityType(), resource, resourceToOperateWithin)) {
                hasPermission = true;
            }
        }

        return hasPermission;
    }

    public static class UserRoleMatcherImpl implements UserRoleMatcher {
        private final SecurityConfig securityConfig;

        public UserRoleMatcherImpl(SecurityConfig securityConfig) {
            this.securityConfig = securityConfig;
        }

        @Override
        public boolean match(CaseInsensitiveString user, CaseInsensitiveString role) {
            return securityConfig.isUserMemberOfRole(user, role);
        }
    }
}
