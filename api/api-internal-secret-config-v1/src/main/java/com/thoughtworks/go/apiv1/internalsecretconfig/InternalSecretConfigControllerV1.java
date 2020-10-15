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

package com.thoughtworks.go.apiv1.internalsecretconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.internalsecretconfig.models.SecretConfigsViewModel;
import com.thoughtworks.go.apiv1.internalsecretconfig.representers.SecretConfigsViewModelRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PluginProfile;
import com.thoughtworks.go.config.SecretConfigs;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.SecretConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.List;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class InternalSecretConfigControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private SecretConfigService secretConfigService;
    private final PipelineConfigService pipelineConfigService;
    private final EnvironmentConfigService environmentConfigService;
    private final PluggableScmService pluggableScmService;
    private final PackageRepositoryService packageRepositoryService;
    private final ClusterProfilesService clusterProfilesService;

    @Autowired
    public InternalSecretConfigControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, SecretConfigService secretConfigService,
                                            PipelineConfigService pipelineConfigService, EnvironmentConfigService environmentConfigService,
                                            PluggableScmService pluggableScmService, PackageRepositoryService packageRepositoryService,
                                            ClusterProfilesService clusterProfilesService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.secretConfigService = secretConfigService;
        this.pipelineConfigService = pipelineConfigService;
        this.environmentConfigService = environmentConfigService;
        this.pluggableScmService = pluggableScmService;
        this.packageRepositoryService = packageRepositoryService;
        this.clusterProfilesService = clusterProfilesService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.SecretConfigsAPI.INTERNAL_BASE;
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

    public String index(Request request, Response response) throws Exception {
        SecretConfigs secretConfigs = secretConfigService.getAllSecretConfigs();
        SecretConfigsViewModel configsViewModel = new SecretConfigsViewModel().setSecretConfigs(secretConfigs);

        List<String> groups = pipelineConfigService.viewableGroupsFor(currentUsername()).stream().map(PipelineConfigs::getGroup).collect(toList());
        List<String> envNames = environmentConfigService.getEnvironmentNames();
        List<String> scms = pluggableScmService.listAllScms().stream().map(SCM::getName).collect(toList());
        List<String> pkgRepos = packageRepositoryService.getPackageRepositories().stream().map(PackageRepository::getName).collect(toList());
        List<String> clusterProfiles = clusterProfilesService.getPluginProfiles().stream().map(PluginProfile::getId).collect(toList());

        configsViewModel.getAutoSuggestions().put("pipeline_group", groups);
        configsViewModel.getAutoSuggestions().put("environment", envNames);
        configsViewModel.getAutoSuggestions().put("pluggable_scm", scms);
        configsViewModel.getAutoSuggestions().put("package_repository", pkgRepos);
        configsViewModel.getAutoSuggestions().put("cluster_profile", clusterProfiles);

        final String etag = etagFor(configsViewModel);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> SecretConfigsViewModelRepresenter.toJSON(writer, configsViewModel));

    }

    private String etagFor(Object entity) {
        return sha512_256Hex(Integer.toString(entity.hashCode()));
    }
}
