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

package com.thoughtworks.go.server.service.permissions.entity;

import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.permissions.PermissionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;

@Component
public class ConfigRepoPermission extends PermissionProvider {
    @Autowired
    public ConfigRepoPermission(GoConfigService goConfigService, SecurityService securityService) {
        super(goConfigService, securityService);
    }

    @Override
    public Map<String, Object> permissions(Username username) {
        List<String> configRepos = config().getConfigRepos().stream().map(RuleAwarePluginProfile::getId).collect(Collectors.toList());
        return getPermissions(configRepos, CONFIG_REPO);
    }

    @Override
    public String name() {
        return CONFIG_REPO.getType();
    }
}
