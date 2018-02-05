/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

public abstract class AbstractAuthenticationHelper {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAuthenticationHelper.class);
    protected final SecurityService securityService;
    protected GoConfigService goConfigService;

    public AbstractAuthenticationHelper(SecurityService securityService, GoConfigService goConfigService) {
        this.securityService = securityService;
        this.goConfigService = goConfigService;
    }

    protected abstract HaltException renderUnauthorizedResponse();

    public void checkUserAnd401(Request req, Response res) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        checkNonAnonymousUser(req, res);
    }

    public void checkNonAnonymousUser(Request req, Response res) {
        if (currentUsername().isAnonymous()) {
            throw renderUnauthorizedResponse();
        }
    }

    public void checkAdminUserAnd401(Request req, Response res) {
        if (!securityService.isUserAdmin(currentUsername())) {
            throw renderUnauthorizedResponse();
        }
    }

    public void checkPipelineGroupOperateUserAnd401(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.hasOperatePermissionForGroup(currentUserLoginName(), groupName)) {
            throw renderUnauthorizedResponse();
        }
    }

    public void checkPipelineGroupAdminUserAnd401(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())) {
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.isUserAdminOfGroup(currentUsername(), groupName)) {
            throw renderUnauthorizedResponse();
        }
    }

    public void checkAnyAdminUserAnd401(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }

        if (!(securityService.isUserAdmin(currentUsername()) || securityService.isUserGroupAdmin(currentUsername()) || securityService.isAuthorizedToViewAndEditTemplates(currentUsername()))) {
            throw renderUnauthorizedResponse();
        }
    }

    private String findPipelineGroupName(Request request) {
        String groupName = request.params("group");
        if (StringUtils.isBlank(groupName)) {
            groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(request.params("pipeline_name")));
        }
        return groupName;
    }

    private Username currentUsername() {
        return UserHelper.getUserName();
    }

    protected CaseInsensitiveString currentUserLoginName() {
        return currentUsername().getUsername();
    }

    public boolean securityEnabled() {
        return securityService.isSecurityEnabled();
    }
}
