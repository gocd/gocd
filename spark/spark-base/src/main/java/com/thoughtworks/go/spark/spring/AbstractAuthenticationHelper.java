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
package com.thoughtworks.go.spark.spring;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.util.List;

import static com.thoughtworks.go.config.exceptions.EntityType.Pipeline;

public abstract class AbstractAuthenticationHelper {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAuthenticationHelper.class);
    protected final SecurityService securityService;
    protected GoConfigService goConfigService;

    public AbstractAuthenticationHelper(SecurityService securityService, GoConfigService goConfigService) {
        this.securityService = securityService;
        this.goConfigService = goConfigService;
    }

    protected abstract HaltException renderForbiddenResponse();

    protected abstract HaltException renderForbiddenResponse(String message);

    public void checkUserAnd403(Request req, Response res) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        checkNonAnonymousUser(req, res);
    }

    public void checkNonAnonymousUser(Request req, Response res) {
        if (currentUsername().isAnonymous()) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAdminUserAnd403(Request req, Response res) {
        if (!securityService.isUserAdmin(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public boolean doesUserHasPermissions(Username username, SupportedAction action, SupportedEntity entity, String resource) {
        return doesUserHasPermissions(username, action, entity, resource, null);
    }

    public boolean doesUserHasPermissions(Username username, SupportedAction action, SupportedEntity entity, String resource, String resourceToOperateWithin) {
        //admin user has access to everything
        if (securityService.isUserAdmin(username)) {
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

    public void checkUserHasPermissions(Username username, SupportedAction action, SupportedEntity entity, String resource) {
        if (!doesUserHasPermissions(username, action, entity, resource, null)) {
            String message = String.format("User '%s' does not have permissions to %s '%s' %s(s).", username.getDisplayName(), action.getAction(), resource, entity.getType());
            throw renderForbiddenResponse(message);
        }
    }

    public void checkUserHasPermissions(Username username, SupportedAction action, SupportedEntity entity, String resource, String resourceToOperateWithin) {
        if (!doesUserHasPermissions(username, action, entity, resource, resourceToOperateWithin)) {
            String message = String.format("User '%s' does not have permissions to %s '%s' %s(s).", username.getDisplayName(), action.getAction(), resource, entity.getType());
            throw renderForbiddenResponse(message);
        }
    }

    public void checkPipelineGroupAdminOfAnyGroup(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }

        if (!securityService.isUserGroupAdmin(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.hasOperatePermissionForGroup(currentUserLoginName(), groupName)) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineGroupAdminOfPipelineOrGroupInURLUserAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.isUserAdminOfGroup(currentUsername(), groupName)) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAdminOrTemplateAdminAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String templateName = request.params("template_name");
        if (StringUtils.isNotBlank(templateName) && !securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString(templateName), currentUsername())) {
            throw renderForbiddenResponse();
        }

        if (StringUtils.isBlank(templateName) && !securityService.isAuthorizedToViewAndEditTemplates(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public void checkViewAccessToTemplateAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }

        String templateName = request.params("template_name");
        if (StringUtils.isNotBlank(templateName) && !securityService.isAuthorizedToViewTemplate(new CaseInsensitiveString(templateName), currentUsername())) {
            throw renderForbiddenResponse();
        }

        if (StringUtils.isBlank(templateName) && !securityService.isAuthorizedToViewTemplates(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineCreationAuthorizationAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }

        JsonElement group = new JsonParser().parse(request.body()).getAsJsonObject().get("group");
        if (group == null) {
            throw new UnprocessableEntityException("Pipeline group must be specified for creating a pipeline.");
        } else {
            String groupName = group.getAsString();
            if (StringUtils.isNotBlank(groupName) && !securityService.isUserAdminOfGroup(currentUsername(), groupName)) {
                throw renderForbiddenResponse();
            }
        }
    }

    public void checkAdminUserOrGroupAdminUserAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()))) {
            throw renderForbiddenResponse();
        }

    }

    public void checkIsAllowedToSeeAnyTemplates403(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()) || securityService.isAuthorizedToViewTemplates(currentUsername()))) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAnyAdminUserAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()) || securityService.isAuthorizedToViewAndEditTemplates(currentUsername()))) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineViewPermissionsAnd403(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        CaseInsensitiveString pipelineName = getPipelineNameFromRequest(request);

        if (!hasViewPermissionWorkaroundForNonExistantPipelineBug_4477(pipelineName, currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    // https://github.com/gocd/gocd/issues/4477
    private boolean hasViewPermissionWorkaroundForNonExistantPipelineBug_4477(CaseInsensitiveString pipelineName,
                                                                              Username username) {
        if (!goConfigService.hasPipelineNamed(pipelineName)) {
            throw new RecordNotFoundException(Pipeline, pipelineName);
        }

        if (securityService.isUserAdmin(username)) {
            return true;
        }

        // we check if pipeline exists because this method returns true in case the group or pipeline does not exist!
        return securityService.hasViewPermissionForPipeline(username, pipelineName.toString());
    }

    private String findPipelineGroupName(Request request) {
        String groupName = request.params("group_name");
        if (StringUtils.isBlank(groupName)) {
            groupName = goConfigService.findGroupNameByPipeline(getPipelineNameFromRequest(request));
        }
        return groupName;
    }

    private CaseInsensitiveString getPipelineNameFromRequest(Request request) {
        String pipelineName = request.params("pipeline_name");
        if (StringUtils.isBlank(pipelineName)) {
            pipelineName = request.queryParams("pipeline_name");
        }
        return new CaseInsensitiveString(pipelineName);
    }

    private Username currentUsername() {
        return SessionUtils.currentUsername();
    }

    protected CaseInsensitiveString currentUserLoginName() {
        return currentUsername().getUsername();
    }

    public boolean securityEnabled() {
        return securityService.isSecurityEnabled();
    }
}
