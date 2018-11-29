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

package com.thoughtworks.go.apiv1.serverdrainmode;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeRepresenter;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.server.service.DrainModeService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class ServerDrainModeControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DrainModeService drainModeService;
    private FeatureToggleService featureToggleService;
    private TimeProvider timeProvider;

    @Autowired
    public ServerDrainModeControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                       DrainModeService drainModeService,
                                       FeatureToggleService featureToggleService,
                                       TimeProvider timeProvider) {
        super(ApiVersion.v1);

        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.drainModeService = drainModeService;
        this.featureToggleService = featureToggleService;
        this.timeProvider = timeProvider;
    }

    @Override
    public String controllerBasePath() {
        return Routes.DrainMode.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before(Routes.DrainMode.SETTINGS, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get(Routes.DrainMode.SETTINGS, mimeType, this::show);
            Spark.patch(Routes.DrainMode.SETTINGS, mimeType, this::patch);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String show(Request req, Response res) throws InvalidPluginTypeException, IOException {
        if (!featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            throw new RecordNotFoundException();
        }

        return writerForTopLevelObject(req, res, writer -> DrainModeRepresenter.toJSON(writer, drainModeService.get()));
    }

    public String patch(Request req, Response res) throws Exception {
        if (!featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            throw new RecordNotFoundException();
        }

        drainModeService.update(buildEntityFromRequestBody(req));
        return show(req, res);
    }

    public ServerDrainMode buildEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return DrainModeRepresenter.fromJSON(jsonReader, currentUsername(), timeProvider, drainModeService.get());
    }

}
