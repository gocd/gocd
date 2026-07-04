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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.config.exceptions.EntityType.Pipeline;
import static spark.utils.StringUtils.isBlank;
import static spark.utils.StringUtils.isNotBlank;

public abstract class AbstractAuthorizationHelper {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAuthorizationHelper.class);
    protected final SecurityService securityService;
    protected GoConfigService goConfigService;

    public AbstractAuthorizationHelper(SecurityService securityService, GoConfigService goConfigService) {
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

    public void checkNonAnonymousUser(@SuppressWarnings("unused") Request req, @SuppressWarnings("unused") Response res) {
        if (currentUsername().isAnonymous()) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAdminUserAnd403(@SuppressWarnings("unused") Request req, @SuppressWarnings("unused") Response res) {
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

    public void checkPipelineGroupAdminViaNameParamsAnd403(Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = getPipelineGroupFrom(request);
        if (!securityService.isUserAdminOfGroup(currentUsername(), groupName)) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineGroupOperateViaNameParamsAnd403(Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = getPipelineGroupFrom(request);
        if (!securityService.hasOperatePermissionForGroup(currentUserLoginName(), groupName)) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAdminOrTemplateAdminAnd403(Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String templateName = request.params("template_name");
        if (isNotBlank(templateName) && !securityService.isAuthorizedToEditTemplate(cis(templateName), currentUsername())) {
            throw renderForbiddenResponse();
        }

        if (isBlank(templateName) && !securityService.isAuthorizedToEditTemplates(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public boolean hasViewPermissionForPipeline(String pipelineName) {
        return securityService.hasViewPermissionForPipeline(currentUsername(), pipelineName);
    }

    public void checkViewAccessToTemplateAnd403(Request request, Response response) {
        checkViewAccessToTemplateAnd403(request, response, r -> r.params("template_name"));
    }

    public void checkViewAccessToTemplateAnd403(Request request, @SuppressWarnings("unused") Response response, Function<Request, String> templateNameExtractor) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }

        String templateName = templateNameExtractor.apply(request);
        if (isNotBlank(templateName) && !securityService.isAuthorizedToViewTemplate(cis(templateName), currentUsername())) {
            throw renderForbiddenResponse();
        }

        if (isBlank(templateName) && !securityService.isAuthorizedToViewTemplates(currentUsername())) {
            throw renderForbiddenResponse();
        }
    }

    public void checkPipelineCreationAuthorizationAnd403(Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }

        JsonElement group = JsonParser.parseString(request.body()).getAsJsonObject().get("group");
        if (group == null) {
            throw new UnprocessableEntityException("Pipeline group must be specified for creating a pipeline.");
        } else {
            String groupName = group.getAsString();
            boolean groupDoesNotExists = !goConfigService.groups().hasGroup(groupName);
            boolean userNotAdminOfTheGrp = !securityService.isUserAdminOfGroup(currentUsername(), groupName);
            if (isNotBlank(groupName) && (groupDoesNotExists || userNotAdminOfTheGrp)) {
                throw renderForbiddenResponse();
            }
        }
    }

    public void checkAnyPipelineGroupAdminUserAnd403(@SuppressWarnings("unused") Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()))) {
            throw renderForbiddenResponse();
        }

    }

    public void checkAnyTemplateViewUserAnd403(@SuppressWarnings("unused") Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()) || securityService.isAuthorizedToViewTemplates(currentUsername()))) {
            throw renderForbiddenResponse();
        }
    }

    public void checkAnyPipelineGroupAdminOrTemplateAdminUserAnd403(@SuppressWarnings("unused") Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!isPipelineGroupAdminOrTemplateAdmin()) {
            throw renderForbiddenResponse();
        }
    }

    public boolean isPipelineGroupAdminOrTemplateAdmin() {
        return securityService.canEditSomeAdminPage(this.currentUsername());
    }

    public void checkPipelineViewPermissionsAnd403(Request request, @SuppressWarnings("unused") Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        CaseInsensitiveString pipelineName = getPipelineNameFrom(request).orElseThrow(this::renderForbiddenResponse);

        // we check if pipeline exists because hasViewPermission returns true in case the group or pipeline does not exist!
        if (!goConfigService.hasPipelineNamed(pipelineName)) {
            throw new RecordNotFoundException(Pipeline, pipelineName);
        }

        Username username = currentUsername();
        if (!(securityService.isUserAdmin(username) || securityService.hasViewPermissionForPipeline(username, pipelineName.toString()))) {
            throw renderForbiddenResponse();
        }
    }

    @VisibleForTesting
    @NotNull String getPipelineGroupFrom(Request request) {
        Optional<String> group = Optional.ofNullable(request.params("group_name")).filter(s -> !s.isBlank())
            .or(() -> Optional.ofNullable(request.queryParams("group_name")).filter(s -> !s.isBlank()));

        Optional<CaseInsensitiveString> pipeline = getPipelineNameFrom(request);

        // Find group implied by pipeline; and if found ensure it is the same as any groupName specified
        return pipeline.map(pn -> {
                String groupFromPipeline = goConfigService.findGroupNameByPipelineOptional(pn)
                    .orElseThrow(() -> new RecordNotFoundException(Pipeline, pn)); // Pipeline name non-blank, but no matching group found

                if (group.isPresent() && !groupFromPipeline.equals(group.get())) {
                    // Group found, but doesn't match request param
                    throw renderForbiddenResponse();
                }
                return groupFromPipeline;
            })
            .orElseGet(() -> group.orElseThrow(this::renderForbiddenResponse)); // Use group name, else throw if missing
    }

    private static @NotNull Optional<CaseInsensitiveString> getPipelineNameFrom(Request request) {
        return Optional.ofNullable(request.params("pipeline_name")).filter(s -> !s.isBlank())
            .or(() -> Optional.ofNullable(request.queryParams("pipeline_name")).filter(s -> !s.isBlank()))
            .map(CaseInsensitiveString::new);
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
