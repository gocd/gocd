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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.UserRoleMatcher;
import com.thoughtworks.go.server.domain.Username;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private GoConfigService goConfigService;

    @Autowired
    public SecurityService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean hasViewPermissionForPipeline(Username username, String pipelineName) {
        String groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        if (groupName == null) {
            return true;
        }
        return hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), groupName);
    }

    public boolean hasViewPermissionForGroup(String userName, String pipelineGroupName) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();

        if (!cruiseConfig.isSecurityEnabled()) {
            return true;
        }

        CaseInsensitiveString username = new CaseInsensitiveString(userName);
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

    public boolean hasOperatePermissionForPipeline(final CaseInsensitiveString username, String pipelineName) {
        String groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        if (groupName == null) {
            return true;
        }
        return hasOperatePermissionForGroup(username, groupName);
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
        CaseInsensitiveString userName = new CaseInsensitiveString(username);

        //TODO - #2517 - stage not exist
        if (stage.hasOperatePermissionDefined()) {
            CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
            String groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
            PipelineConfigs group = goConfigService.getCurrentConfig().findGroup(groupName);
            if (isUserAdmin(new Username(userName)) || isUserAdminOfGroup(userName, group)) {
                return true;
            }
            return goConfigService.readAclBy(pipelineName, stageName).isGranted(userName);
        }

        return hasOperatePermissionForPipeline(new CaseInsensitiveString(username), pipelineName);
    }

    public boolean isUserAdmin(Username username) {
        return goConfigService.isUserAdmin(username);
    }

    public boolean isSecurityEnabled(){
        return goConfigService.isSecurityEnabled();
    }

    public boolean hasOperatePermissionForFirstStage(String pipelineName, String userName) {
        StageConfig stage = goConfigService.findFirstStageOfPipeline(new CaseInsensitiveString(pipelineName));
        return hasOperatePermissionForStage(pipelineName, CaseInsensitiveString.str(stage.name()), userName);
    }

    public boolean canViewAdminPage(Username username) {
        return isUserAdmin(username) || isUserGroupAdmin(username) || isAuthorizedToViewAndEditTemplates(username);
    }

    public boolean canCreatePipelines(Username username) {
        return isUserAdmin(username) || isUserGroupAdmin(username);
    }

    public boolean hasOperatePermissionForAgents(Username username) {
        return isUserAdmin(username);
    }

    public boolean hasViewOrOperatePermissionForPipeline(Username username, String pipelineName) {
        return hasViewPermissionForPipeline(username, pipelineName) ||
                hasOperatePermissionForPipeline(username.getUsername(), pipelineName);
    }

    public String logoutSuccessUrl() {
        return "/";
    }

    public String casServiceBaseUrl() {
        return goConfigService.serverConfig().getSiteUrlPreferablySecured().getUrl();
    }

    public List<CaseInsensitiveString> viewablePipelinesFor(Username username) {
        List<CaseInsensitiveString> pipelines = new ArrayList<>();
        for (String group : goConfigService.allGroups()) {
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
            return goConfigService.allGroups();
        }
        List<String> modifiableGroups = new ArrayList<>();
        for (String group : goConfigService.allGroups()) {
            if (isUserAdminOfGroup(userName.getUsername(), group)) {
                modifiableGroups.add(group);
            }
        }
        return modifiableGroups;
    }

    public boolean isAuthorizedToViewAndEditTemplates(Username username) {
        return goConfigService.isAuthorizedToViewAndEditTemplates(username);
    }

    public boolean isAuthorizedToEditTemplate(String templateName, Username username) {
        return goConfigService.isAuthorizedToEditTemplate(templateName, username);
    }

    public static class UserRoleMatcherImpl implements UserRoleMatcher {
        private final SecurityConfig securityConfig;

        public UserRoleMatcherImpl(SecurityConfig securityConfig) {
            this.securityConfig = securityConfig;
        }

        public boolean match(CaseInsensitiveString user, CaseInsensitiveString role) {
            return securityConfig.isUserMemberOfRole(user, role);
        }
    }

}
