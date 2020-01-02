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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.domain.PipelineGroupVisitor;

public class SecurityFilter implements PipelineGroupVisitor {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final PipelineGroupVisitor visitor;
    private String userName;

    public SecurityFilter(PipelineGroupVisitor visitor, GoConfigService goConfigService, SecurityService securityService, String userName) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.visitor = visitor;
        this.userName = userName;
    }

    @Override
    public void visit(PipelineConfigs group) {
        if (hasViewPermission(group.getGroup()) || isUserAGroupAdmin(group)) {
            visitor.visit(group);
        }
    }

    private boolean isUserAGroupAdmin(PipelineConfigs group) {
        return group.getAuthorization().isUserAnAdmin(new CaseInsensitiveString(userName), goConfigService.rolesForUser(new CaseInsensitiveString(userName)));
    }

    private boolean hasViewPermission(String groupName) {
        return securityService.hasViewPermissionForGroup(userName, groupName);
    }
}
