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
package com.thoughtworks.go.apiv4.configrepos;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.abstractmaterialtest.AbstractMaterialTestController;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv4.configrepos.representers.ConfigRepoWithResultListRepresenter;
import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.Routes.ConfigRepos;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;
import static com.thoughtworks.go.server.util.DigestMixin.digestMany;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class ConfigReposInternalControllerV4 extends AbstractMaterialTestController implements SparkSpringController {

    private final ConfigRepoService service;
    private final GoConfigRepoConfigDataSource dataSource;
    private final ApiAuthenticationHelper authHelper;
    private final MaterialUpdateService mus;
    private final MaterialConfigConverter converter;
    private final EnvironmentConfigService environmentConfigService;
    private final PipelineConfigsService pipelineConfigsService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public ConfigReposInternalControllerV4(ApiAuthenticationHelper authHelper, ConfigRepoService service, GoConfigRepoConfigDataSource dataSource, MaterialUpdateService mus, MaterialConfigConverter converter, EnvironmentConfigService environmentConfigService, PipelineConfigsService pipelineConfigsService,
                                           GoConfigService goConfigService, PasswordDeserializer passwordDeserializer, MaterialConfigConverter materialConfigConverter, SystemEnvironment systemEnvironment, SecretParamResolver secretParamResolver, EntityHashingService entityHashingService) {
        super(ApiVersion.v4, goConfigService, passwordDeserializer, materialConfigConverter, systemEnvironment, secretParamResolver);
        this.service = service;
        this.dataSource = dataSource;
        this.authHelper = authHelper;
        this.mus = mus;
        this.converter = converter;
        this.environmentConfigService = environmentConfigService;
        this.pipelineConfigsService = pipelineConfigsService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return ConfigRepos.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, authHelper::checkUserAnd403);
            before("", mimeType, this::verifyContentType);

            before(Routes.ConfigRepos.CHECK_CONNECTION_PATH, mimeType, this::authorize);

            get(ConfigRepos.INDEX_PATH, mimeType, this::listRepos);
            post(Routes.ConfigRepos.CHECK_CONNECTION_PATH, mimeType, this::testConnection);
        });
    }

    String listRepos(Request req, Response res) throws IOException {
        List<ConfigRepoWithResult> userSpecificRepos = allRepos().stream()
                .filter(repo -> authHelper.doesUserHasPermissions(currentUsername(), getAction(req), CONFIG_REPO, repo.repo().getId()))
                .collect(toList());

        final String etag = etagFor(userSpecificRepos);

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }
        Map<String, List<String>> autoSuggestions = new HashMap<>();
        ArrayList<String> pipelineNames = new ArrayList<>();
        ArrayList<String> pipelineGroupNames = new ArrayList<>();

        List<String> envNames = environmentConfigService.getEnvironmentNames();
        List<PipelineConfigs> groupsForUser = pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString());
        groupsForUser.forEach((grp) -> {
            pipelineGroupNames.add(grp.getGroup());
            pipelineNames.addAll(grp.getPipelines().stream().map((pipelineConfig) -> pipelineConfig.name().toString()).collect(toList()));
        });

        autoSuggestions.put("environment", envNames);
        autoSuggestions.put("pipeline_group", pipelineGroupNames);
        autoSuggestions.put("pipeline", pipelineNames);

        return writerForTopLevelObject(req, res, w -> ConfigRepoWithResultListRepresenter.toJSON(w, userSpecificRepos, autoSuggestions));
    }

    private String etagFor(List<ConfigRepoWithResult> entity) {
        return digestMany(entity.stream().map(this::etagFor).toArray(String[]::new));
    }

    private String etagFor(ConfigRepoWithResult entity) {
        return digestMany(
                entityHashingService.hashForEntity(entity.repo()),
                entityHashingService.hashForEntity(entity.result())
        );
    }

    private List<ConfigRepoWithResult> allRepos() {
        return service.getConfigRepos().stream().map(r -> {
            PartialConfigParseResult result = dataSource.getLastParseResult(r.getRepo());
            return new ConfigRepoWithResult(r, result, isMaterialUpdateInProgress(r));
        }).collect(toList());
    }

    private boolean isMaterialUpdateInProgress(ConfigRepoConfig configRepoConfig) {
        return mus.isInProgress(converter.toMaterial(configRepoConfig.getRepo()));
    }

    private void authorize(Request request, Response response) {
        authHelper.checkUserHasPermissions(currentUsername(), getAction(request), CONFIG_REPO, request.params(":id"));
    }
}
