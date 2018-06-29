/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.datasharing.settings.representers.DataSharingSettingsRepresenter;
import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.service.DataSharingSettingsService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.util.TimeProvider;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

public class DataSharingSettingsControllerV1Delegate extends ApiController implements CrudController<DataSharingSettings> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DataSharingSettingsService dataSharingSettingsService;
    private final EntityHashingService entityHashingService;
    private final TimeProvider timeProvider;

    public DataSharingSettingsControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper,
                                                   DataSharingSettingsService dataSharingSettingsService,
                                                   EntityHashingService entityHashingService,
                                                   TimeProvider timeProvider) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dataSharingSettingsService = dataSharingSettingsService;
        this.entityHashingService = entityHashingService;
        this.timeProvider = timeProvider;
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
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);
            before("", mimeType, this::checkAdminUserAnd403OnlyForPatch);
            get("", this::getDataSharingSettings);
            patch("", mimeType, this::patchDataSharingSettings);
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
        if (!isPutRequestFresh(request, dataSharingSettingsService.get())) {
            throw haltBecauseEtagDoesNotMatch();
        }
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingSettingsService.createOrUpdate(getEntityFromRequestBody(request));
        return handleCreateOrUpdateResponse(request, response, dataSharingSettingsService.get(), result);
    }

    @Override
    public String etagFor(DataSharingSettings entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public DataSharingSettings doGetEntityFromConfig(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSharingSettings getEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return DataSharingSettingsRepresenter.fromJSON(jsonReader, currentUsername(), timeProvider, dataSharingSettingsService.get());
    }

    @Override
    public String jsonize(Request request, DataSharingSettings dataSharingSettings) {
        return jsonizeAsTopLevelObject(request, writer -> DataSharingSettingsRepresenter.toJSON(writer, dataSharingSettings));
    }

    @Override
    public JsonNode jsonNode(Request request, DataSharingSettings dataSharingSettings) throws IOException {
        return new ObjectMapper().readTree(jsonize(request, dataSharingSettings));
    }
}
