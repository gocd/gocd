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
package com.thoughtworks.go.apiv1.featuretoggles;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.featuretoggles.representers.FeatureTogglesRepresenter;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class FeatureTogglesControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private FeatureToggleService featureToggleService;

    @Autowired
    public FeatureTogglesControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, FeatureToggleService featureToggleService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.FeatureToggle.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            put(Routes.FeatureToggle.FEATURE_TOGGLE_KEY, mimeType, this::update);
        });
    }

    public String index(Request request, Response response) throws IOException {
        FeatureToggles featureToggles = featureToggleService.allToggles();
        return writerForTopLevelArray(request, response, outputWriter -> FeatureTogglesRepresenter.toJSON(outputWriter, featureToggles));
    }

    public String update(Request request, Response response) throws IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        String toggleValue = jsonReader.getString("toggle_value");

        if (!(StringUtils.equalsIgnoreCase("on", toggleValue) || StringUtils.equalsIgnoreCase("off", toggleValue))) {
            throw new UnprocessableEntityException("Value of property \"toggle_value\" is invalid. Valid values are: \"on\" and \"off\".");
        }

        featureToggleService.changeValueOfToggle(request.params("toggle_key"), toggleValue.equalsIgnoreCase("on"));
        return writerForTopLevelObject(request, response, writer -> writer.add("message", "success"));
    }
}
