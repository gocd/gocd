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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.*;

@Service
public class PermissionsService {
    private final GoConfigService goConfigService;
    private SecurityService securityService;

    @Autowired
    public PermissionsService(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public Map<String, Object> getPermissionsForCurrentUser() {
        List<String> environments = config().getEnvironments().stream().map(env -> env.name().toString()).collect(Collectors.toList());
        List<String> configRepos = config().getConfigRepos().stream().map(RuleAwarePluginProfile::getId).collect(Collectors.toList());
        List<String> clusterProfiles = config().getElasticConfig().getClusterProfiles().stream().map(ClusterProfile::getId).collect(Collectors.toList());

        Map<String, Object> json = new LinkedHashMap<>();
        json.put(ENVIRONMENT.getType(), getPermissions(environments, ENVIRONMENT));
        json.put(CONFIG_REPO.getType(), getPermissions(configRepos, CONFIG_REPO));
        json.put(CLUSTER_PROFILE.getType(), getPermissions(clusterProfiles, CLUSTER_PROFILE));
        json.put(ELASTIC_AGENT_PROFILE.getType(), addElasticProfilesPermissions());

        return json;
    }

    private Map<String, Object> getPermissions(List<String> entities, SupportedEntity entityType) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();

        json.put(VIEW.getAction(), entities.stream().filter(resource -> canView(entityType, resource)).collect(Collectors.toList()));
        json.put(ADMINISTER.getAction(), entities.stream().filter(resource -> canAdminister(entityType, resource)).collect(Collectors.toList()));

        return json;
    }

    private Map<String, Object> addElasticProfilesPermissions() {
        SupportedEntity entityType = ELASTIC_AGENT_PROFILE;
        List<ElasticProfile> elasticProfiles = config().getElasticConfig().getProfiles();

        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put(VIEW.getAction(), elasticProfiles.stream().filter(profile -> canView(entityType, profile.getId(), profile.getClusterProfileId())).map(ElasticProfile::getId).collect(Collectors.toList()));
        json.put(ADMINISTER.getAction(), elasticProfiles.stream().filter(profile -> canAdminister(entityType, profile.getId(), profile.getClusterProfileId())).map(ElasticProfile::getId).collect(Collectors.toList()));

        return json;
    }

    private CruiseConfig config() {
        return goConfigService.getMergedConfigForEditing();
    }

    private boolean canView(SupportedEntity entity, String resource) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, entity, resource, null);
    }

    private boolean canView(SupportedEntity entity, String resource, String resourceToOperateWithin) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, entity, resource, resourceToOperateWithin);
    }

    private boolean canAdminister(SupportedEntity entity, String resource) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, entity, resource, null);
    }

    private boolean canAdminister(SupportedEntity entity, String resource, String resourceToOperateWithin) {
        return securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, entity, resource, resourceToOperateWithin);
    }
}
