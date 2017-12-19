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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.HaltException;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.api.util.HaltResponses.haltBecauseUnauthorized;

@Component
public class AuthenticationHelper {

    private GoConfigService goConfigService;
    private final SecurityService securityService;
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationHelper.class);

    @Autowired
    public AuthenticationHelper(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public void checkAdminUserAnd401(Request req, Response res) {
        if (!securityService.isUserAdmin(currentUsername())) {
            throw becauseUnauthorized();
        }
    }

    public void checkPipelineGroupOperateUserAnd401(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())){
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.hasOperatePermissionForGroup(currentUserLoginName(), groupName)) {
            throw becauseUnauthorized();
        }
    }

    public void checkPipelineGroupAdminUserAnd401(Request request, Response response) {
        if (!securityService.isSecurityEnabled() || securityService.isUserAdmin(currentUsername())){
            return;
        }
        String groupName = findPipelineGroupName(request);
        if (!securityService.isUserAdminOfGroup(currentUsername(), groupName)) {
            throw becauseUnauthorized();
        }
    }


    private String findPipelineGroupName(Request request) {
        String groupName = request.params("group");
        if (StringUtils.isBlank(groupName)) {
            groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(request.params("pipeline_name")));
        }
        return groupName;
    }

    private HaltException becauseUnauthorized() {
        LOG.info("User {} attempted to perform an unauthorized action!", currentUserLoginName());
        return haltBecauseUnauthorized();
    }

    private Username currentUsername() {
        return UserHelper.getUserName();
    }

    private CaseInsensitiveString currentUserLoginName() {
        return currentUsername().getUsername();
    }

}
