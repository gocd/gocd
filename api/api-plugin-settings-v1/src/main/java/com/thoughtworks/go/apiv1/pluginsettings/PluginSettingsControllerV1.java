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
package com.thoughtworks.go.apiv1.pluginsettings;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.pluginsettings.representers.PluginSettingsRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.PluginService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

@Component
public class PluginSettingsControllerV1 extends ApiController implements SparkSpringController, CrudController<PluginSettings> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PluginService pluginService;
    private EntityHashingService entityHashingService;
    private final String PLUGIN_ID_KEY = "plugin_id";

    @Autowired
    public PluginSettingsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, PluginService pluginService, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pluginService = pluginService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PluginSettingsAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get(Routes.PluginSettingsAPI.ID, mimeType, this::show);

            post("", mimeType, this::create);
            put(Routes.PluginSettingsAPI.ID, mimeType, this::update);
        });
    }

    public String show(Request req, Response res) throws IOException {
        String pluginId = req.params(PLUGIN_ID_KEY);
        PluginSettings pluginSettings = fetchEntityFromConfig(pluginId);
        if (pluginSettings == null) {
            throw new RecordNotFoundException(EntityType.PluginSettings, pluginId);
        }

        if (isGetOrHeadRequestFresh(req, pluginSettings)) {
            return notModified(res);
        } else {
            setEtagHeader(pluginSettings, res);
            return writerForTopLevelObject(req, res, jsonWriter(pluginSettings));
        }
    }

    public String create(Request req, Response res) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginSettings pluginSettings = buildEntityFromRequestBody(req);
        pluginService.createPluginSettings(pluginSettings, currentUsername(), result);
        return handleCreateOrUpdateResponse(req, res, pluginSettings, result);
    }

    public String update(Request req, Response res) {
        String pluginId = req.params(PLUGIN_ID_KEY);
        PluginSettings existingPluginSettings = fetchEntityFromConfig(pluginId);
        PluginSettings pluginSettingsToCreate = buildEntityFromRequestBody(req);

        if (isPutRequestStale(req, existingPluginSettings)) {
            throw haltBecauseEtagDoesNotMatch();
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pluginService.updatePluginSettings(pluginSettingsToCreate, currentUsername(), result, etagFor(existingPluginSettings));
        return handleCreateOrUpdateResponse(req, res, pluginSettingsToCreate, result);
    }

    @Override
    public String etagFor(PluginSettings entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PluginSettings;
    }

    @Override
    public PluginSettings doFetchEntityFromConfig(String name) {
        return pluginService.getPluginSettings(name);
    }

    @Override
    public PluginSettings buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return PluginSettingsRepresenter.fromJSON(loadPluginInfo(jsonReader.getString(PLUGIN_ID_KEY)), jsonReader);
    }

    private PluginInfo loadPluginInfo(String pluginId) {
        if (!pluginService.isPluginLoaded(pluginId)) {
            throw new UnprocessableEntityException(String.format("The plugin with id '%s' is not loaded.", pluginId));
        }

        PluginInfo pluginInfo = pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginId);

        if (pluginInfo == null) {
            throw new UnprocessableEntityException(String.format("The plugin with id '%s' does not support plugin-settings.", pluginId));
        }

        return pluginInfo;
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PluginSettings pluginSettings) {
        return writer -> PluginSettingsRepresenter.toJSON(writer, pluginSettings);
    }
}
