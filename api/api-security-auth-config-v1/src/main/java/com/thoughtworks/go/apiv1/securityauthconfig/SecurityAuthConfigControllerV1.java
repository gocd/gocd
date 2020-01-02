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
package com.thoughtworks.go.apiv1.securityauthconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.securityauthconfig.representers.SecurityAuthConfigRepresenter;
import com.thoughtworks.go.apiv1.securityauthconfig.representers.SecurityAuthConfigsRepresenter;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.CachedDigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

@Component
public class SecurityAuthConfigControllerV1 extends ApiController implements SparkSpringController, CrudController<SecurityAuthConfig> {

    private SecurityAuthConfigService securityAuthConfigService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private EntityHashingService entityHashingService;

    @Autowired
    public SecurityAuthConfigControllerV1(SecurityAuthConfigService securityAuthConfigService,
                                          ApiAuthenticationHelper apiAuthenticationHelper,
                                          EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.securityAuthConfigService = securityAuthConfigService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.SecurityAuthConfigAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.SecurityAuthConfigAPI.ID, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.SecurityAuthConfigAPI.ID, mimeType, this::update);
            delete(Routes.SecurityAuthConfigAPI.ID, mimeType, this::deleteAuthConfig);
        });
    }

    public String index(Request request, Response response) throws IOException {
        SecurityAuthConfigs securityAuthConfigs = securityAuthConfigService.getPluginProfiles();

        setEtagHeader(response, etagFor(securityAuthConfigs));

        return writerForTopLevelObject(request, response, writer -> SecurityAuthConfigsRepresenter.toJSON(writer, securityAuthConfigs));
    }

    public String show(Request request, Response response) throws IOException {
        final SecurityAuthConfig securityAuthConfig = fetchEntityFromConfig(request.params("id"));
        String etag = etagFor(securityAuthConfig);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, jsonWriter(securityAuthConfig));
    }

    public String create(Request request, Response response) {
        SecurityAuthConfig securityAuthConfig = buildEntityFromRequestBody(request);
        haltIfEntityWithSameIdExists(securityAuthConfig);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        securityAuthConfigService.create(currentUsername(), securityAuthConfig, result);

        return handleCreateOrUpdateResponse(request, response, securityAuthConfig, result);
    }

    public String update(Request request, Response response) {
        final String securityAuthConfigId = request.params("id");
        final SecurityAuthConfig existingAuthConfig = fetchEntityFromConfig(securityAuthConfigId);
        final SecurityAuthConfig newAuthConfig = buildEntityFromRequestBody(request);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (isRenameAttempt(securityAuthConfigId, newAuthConfig.getId())) {
            throw haltBecauseRenameOfEntityIsNotSupported(getEntityType().getEntityNameLowerCase());
        }

        if (isPutRequestStale(request, existingAuthConfig)) {
            throw haltBecauseEtagDoesNotMatch(getEntityType().getEntityNameLowerCase(), existingAuthConfig.getId());
        }

        newAuthConfig.setId(securityAuthConfigId);
        securityAuthConfigService.update(currentUsername(), etagFor(existingAuthConfig), newAuthConfig, result);
        return handleCreateOrUpdateResponse(request, response, newAuthConfig, result);
    }

    public String deleteAuthConfig(Request request, Response response) {
        SecurityAuthConfig securityAuthConfig = fetchEntityFromConfig(request.params("id"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        securityAuthConfigService.delete(currentUsername(), securityAuthConfig, result);

        return handleSimpleMessageResponse(response, result);
    }

    @Override
    public String etagFor(SecurityAuthConfig entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    private String etagFor(SecurityAuthConfigs securityAuthConfigs) {
        String md5OfAllAuthConfigs = securityAuthConfigs.stream()
                .map(this::etagFor)
                .collect(Collectors.joining("/"));

        return CachedDigestUtils.md5Hex(md5OfAllAuthConfigs);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SecurityAuthConfig;
    }

    @Override
    public SecurityAuthConfig doFetchEntityFromConfig(String id) {
        return securityAuthConfigService.findProfile(id);
    }

    @Override
    public SecurityAuthConfig buildEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return SecurityAuthConfigRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(SecurityAuthConfig securityAuthConfig) {
        return outputWriter -> SecurityAuthConfigRepresenter.toJSON(outputWriter, securityAuthConfig);
    }

    private boolean isRenameAttempt(String profileIdFromRequestParam, String profileIdFromRequestBody) {
        if (StringUtils.isBlank(profileIdFromRequestBody)) {
            return false;
        }
        return !StringUtils.equals(profileIdFromRequestBody, profileIdFromRequestParam);
    }

    private void haltIfEntityWithSameIdExists(SecurityAuthConfig securityAuthConfig) {
        if (doFetchEntityFromConfig(securityAuthConfig.getId()) == null) {
            return;
        }

        securityAuthConfig.addError("id", getEntityType().alreadyExists(securityAuthConfig.getId()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(securityAuthConfig), getEntityType().getEntityNameLowerCase(), securityAuthConfig.getId());
    }
}
