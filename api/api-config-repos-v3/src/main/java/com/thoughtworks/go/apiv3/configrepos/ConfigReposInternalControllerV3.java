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
package com.thoughtworks.go.apiv3.configrepos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv3.configrepos.representers.ConfigRepoWithResultListRepresenter;
import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.spark.Routes.ConfigRepos;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;
import static com.thoughtworks.go.util.CachedDigestUtils.sha256Hex;
import static spark.Spark.*;

@Component
public class ConfigReposInternalControllerV3 extends ApiController implements SparkSpringController {

    private final ConfigRepoService service;
    private final GoRepoConfigDataSource dataSource;
    private final ApiAuthenticationHelper authHelper;
    private final MaterialUpdateService mus;
    private final MaterialConfigConverter converter;

    @Autowired
    public ConfigReposInternalControllerV3(ApiAuthenticationHelper authHelper, ConfigRepoService service, GoRepoConfigDataSource dataSource, MaterialUpdateService mus, MaterialConfigConverter converter) {
        super(ApiVersion.v3);
        this.service = service;
        this.dataSource = dataSource;
        this.authHelper = authHelper;
        this.mus = mus;
        this.converter = converter;
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

            get(ConfigRepos.INDEX_PATH, mimeType, this::listRepos);
        });
    }

    String listRepos(Request req, Response res) throws IOException {
        List<ConfigRepoWithResult> userSpecificRepos = allRepos().stream()
                .filter(repo -> authHelper.doesUserHasPermissions(currentUsername(), getAction(req), CONFIG_REPO, repo.repo().getId()))
                .collect(Collectors.toList());

        final String etag = etagFor(userSpecificRepos);

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }
        Function<String, Boolean> canUserAdministerConfigRepo = repoId -> authHelper.doesUserHasPermissions(currentUsername(), SupportedAction.ADMINISTER, CONFIG_REPO, repoId);
        return writerForTopLevelObject(req, res, w -> ConfigRepoWithResultListRepresenter.toJSON(w, userSpecificRepos, canUserAdministerConfigRepo));
    }

    private String etagFor(Object entity) {
        return sha256Hex(Integer.toString(entity.hashCode()));
    }

    private List<ConfigRepoWithResult> allRepos() {
        return service.getConfigRepos().stream().map(r -> {
            PartialConfigParseResult result = dataSource.getLastParseResult(r.getRepo());
            return new ConfigRepoWithResult(r, result, isMaterialUpdateInProgress(r));
        }).collect(Collectors.toList());
    }

    private boolean isMaterialUpdateInProgress(ConfigRepoConfig configRepoConfig) {
        return mus.isInProgress(converter.toMaterial(configRepoConfig.getRepo()));
    }
}
