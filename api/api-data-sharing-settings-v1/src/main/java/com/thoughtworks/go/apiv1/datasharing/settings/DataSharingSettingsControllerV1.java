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
package com.thoughtworks.go.apiv1.datasharing.settings;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.datasharing.settings.representers.DataSharingSettingsRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.datasharing.DataSharingNotification;
import com.thoughtworks.go.server.service.datasharing.DataSharingSettingsService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class DataSharingSettingsControllerV1 extends ApiController implements SparkSpringController, CrudController<DataSharingSettings> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DataSharingSettingsService dataSharingSettingsService;
    private final EntityHashingService entityHashingService;
    private final TimeProvider timeProvider;
    private final DataSharingNotification dataSharingNotification;

    @Autowired
    public DataSharingSettingsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                           DataSharingSettingsService dataSharingSettingsService,
                                           EntityHashingService entityHashingService,
                                           TimeProvider timeProvider, DataSharingNotification dataSharingNotification) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dataSharingSettingsService = dataSharingSettingsService;
        this.entityHashingService = entityHashingService;
        this.timeProvider = timeProvider;
        this.dataSharingNotification = dataSharingNotification;
    }

    @Override
    public String controllerBasePath() {
        return DataSharing.SETTINGS_PATH;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);
            before("", mimeType, this::checkAdminUserAnd403OnlyForPatch);
            before("/notification_auth", mimeType, apiAuthenticationHelper::checkUserAnd403);
            get("", mimeType, this::getDataSharingSettings);
            patch("", mimeType, this::patchDataSharingSettings);
            get("/notification_auth", mimeType, this::getDataSharingNotificationForCurrentUser);
        });
    }

    private void checkAdminUserAnd403OnlyForPatch(Request request, Response response) {
        if ("PATCH".equals(request.requestMethod())) {
            apiAuthenticationHelper.checkAdminUserAnd403(request, response);
        }
    }

    public String getDataSharingSettings(Request request, Response response) {
        DataSharingSettings dataSharingSettings = dataSharingSettingsService.get();
        setEtagHeader(response, etagFor(dataSharingSettings));
        return jsonize(request, dataSharingSettings);
    }

    public String patchDataSharingSettings(Request request, Response response) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingSettingsService.createOrUpdate(buildEntityFromRequestBody(request));
        return handleCreateOrUpdateResponse(request, response, dataSharingSettingsService.get(), result);
    }

    public String getDataSharingNotificationForCurrentUser(Request request, Response response) {
        boolean shouldAllow = dataSharingNotification.allowNotificationFor(currentUsername());
        return jsonizeAsTopLevelObject(request, writer -> writer.add("show_notification", shouldAllow));
    }

    @Override
    public String etagFor(DataSharingSettings entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        throw new UnsupportedOperationException("Not implemented. Unlike other entities, data sharing has a single representation in the config.");
    }

    @Override
    public DataSharingSettings doFetchEntityFromConfig(String name) {
        throw new UnsupportedOperationException("Not implemented. Unlike other entities, data sharing has a single representation in the config.");
    }

    @Override
    public DataSharingSettings buildEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return DataSharingSettingsRepresenter.fromJSON(jsonReader, currentUsername(), timeProvider, dataSharingSettingsService.get());
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(DataSharingSettings dataSharingSettings) {
        return writer -> DataSharingSettingsRepresenter.toJSON(writer, dataSharingSettings);
    }
}
