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
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeInfoRepresenter;
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeSettingsRepresenter;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.server.service.DrainModeService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static spark.Spark.*;

@Component
public class ServerDrainModeControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DrainModeService drainModeService;
    private JobInstanceService jobInstanceService;
    private FeatureToggleService featureToggleService;
    private Clock clock;

    @Autowired
    public ServerDrainModeControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                       DrainModeService drainModeService,
                                       JobInstanceService jobInstanceService,
                                       FeatureToggleService featureToggleService,
                                       Clock clock) {
        super(ApiVersion.v1);

        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.drainModeService = drainModeService;
        this.jobInstanceService = jobInstanceService;
        this.featureToggleService = featureToggleService;
        this.clock = clock;
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
            before(Routes.DrainMode.INFO, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get(Routes.DrainMode.SETTINGS, mimeType, this::show);
            post(Routes.DrainMode.SETTINGS, mimeType, this::updateDrainModeState);

            get(Routes.DrainMode.INFO, mimeType, this::getDrainModeInfo);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String show(Request req, Response res) throws InvalidPluginTypeException, IOException {
        if (!featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            throw new RecordNotFoundException();
        }

        return writerForTopLevelObject(req, res, writer -> DrainModeSettingsRepresenter.toJSON(writer, drainModeService.get()));
    }

    public String updateDrainModeState(Request req, Response res) throws Exception {
        if (!featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            throw new RecordNotFoundException();
        }

        final JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

        final ServerDrainMode serverDrainMode = new ServerDrainMode(jsonReader.getBoolean("drain"), currentUserLoginName().toString(), clock.currentTime());

        drainModeService.update(serverDrainMode);
        return show(req, res);
    }

    public String getDrainModeInfo(Request req, Response res) throws InvalidPluginTypeException, IOException {
        if (!featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            throw new RecordNotFoundException();
        }

        Collection<DrainModeService.MaterialPerformingMDU> runningMDUs = drainModeService.getRunningMDUs();
        List<JobInstance> jobInstances = jobInstanceService.allRunningJobs();
        boolean isServerCompletelyDrained = runningMDUs.isEmpty() && jobInstances.isEmpty();
        return writerForTopLevelObject(req, res, writer -> DrainModeInfoRepresenter.toJSON(writer, isServerCompletelyDrained, runningMDUs, jobInstances));
    }
}
