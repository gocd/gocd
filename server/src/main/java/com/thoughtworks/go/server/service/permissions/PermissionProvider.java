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

package com.thoughtworks.go.server.service.permissions;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;

public abstract class PermissionProvider {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    public PermissionProvider(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public abstract Map<String, Object> permissions(Username username);

    public abstract String name();

    public CruiseConfig config() {
        return goConfigService.getMergedConfigForEditing();
    }

    protected Map<String, Object> getPermissions(List<String> entities, SupportedEntity entityType) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();

        json.put(VIEW.getAction(), entities.stream().filter(resource -> canView(entityType, resource)).collect(Collectors.toList()));
        json.put(ADMINISTER.getAction(), entities.stream().filter(resource -> canAdminister(entityType, resource)).collect(Collectors.toList()));

        return json;
    }

    private boolean canView(SupportedEntity entity, String resource) {
        return canView(entity, resource, null);
    }

    private boolean canAdminister(SupportedEntity entity, String resource) {
        return canAdminister(entity, resource, null);
    }

    public boolean canView(SupportedEntity entity, String resource, String resourceToOperateWithin) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, entity, resource, resourceToOperateWithin);
    }

    public boolean canAdminister(SupportedEntity entity, String resource, String resourceToOperateWithin) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, entity, resource, resourceToOperateWithin);
    }

}
