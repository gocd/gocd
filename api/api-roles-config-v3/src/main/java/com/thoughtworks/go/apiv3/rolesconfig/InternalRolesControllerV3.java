/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv3.rolesconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv3.rolesconfig.models.RolesViewModel;
import com.thoughtworks.go.apiv3.rolesconfig.representers.RolesViewModelRepresenter;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class InternalRolesControllerV3 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final RoleConfigService roleConfigService;
    private final EnvironmentConfigService environmentConfigService;
    private final ConfigRepoService configRepoService;
    private final ElasticProfileService elasticProfileService;
    private final ClusterProfilesService clusterProfilesService;

    @Autowired
    public InternalRolesControllerV3(ApiAuthenticationHelper apiAuthenticationHelper,
                                     RoleConfigService roleConfigService,
                                     EnvironmentConfigService environmentConfigService,
                                     ConfigRepoService configRepoService,
                                     ElasticProfileService elasticProfileService,
                                     ClusterProfilesService clusterProfilesService) {
        super(ApiVersion.v3);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.roleConfigService = roleConfigService;
        this.environmentConfigService = environmentConfigService;
        this.configRepoService = configRepoService;
        this.elasticProfileService = elasticProfileService;
        this.clusterProfilesService = clusterProfilesService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Roles.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
        });
    }

    String index(Request request, Response response) throws IOException {
        String pluginType = request.queryParams("type");
        RolesConfig roles = roleConfigService.getRoles().ofType(pluginType);
        RolesViewModel rolesViewModel = new RolesViewModel().setRolesConfig(roles);

        List<String> envNames = environmentConfigService.getEnvironmentNames();
        List<String> configRepoNames = configRepoService.getConfigRepos().stream().map(ConfigRepoConfig::getId).collect(toList());
        List<String> clusterProfileIds = clusterProfilesService.getPluginProfiles().stream().map(ClusterProfile::getId).collect(toList());
        List<String> elasticAgentProfileIds = elasticProfileService.getPluginProfiles().stream().map(ElasticProfile::getId).collect(toList());

        rolesViewModel.getAutoSuggestions().put("environment", envNames);
        rolesViewModel.getAutoSuggestions().put("config_repo", configRepoNames);
        rolesViewModel.getAutoSuggestions().put("cluster_profile", clusterProfileIds);
        rolesViewModel.getAutoSuggestions().put("elastic_agent_profile", elasticAgentProfileIds);

        return writerForTopLevelObject(request, response, (outputWriter) -> RolesViewModelRepresenter.toJSON(outputWriter, rolesViewModel));
    }
}

