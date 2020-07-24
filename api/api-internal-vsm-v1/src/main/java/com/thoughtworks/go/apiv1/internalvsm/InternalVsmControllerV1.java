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

package com.thoughtworks.go.apiv1.internalvsm;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.internalvsm.representers.VSMRepresenter;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.ValueStreamMapService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.server.service.ServiceConstants.VSM.BAD_PIPELINE_COUNTER_MSG;
import static spark.Spark.*;

@Component
public class InternalVsmControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ValueStreamMapService valueStreamMapService;

    @Autowired
    public InternalVsmControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, ValueStreamMapService valueStreamMapService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.valueStreamMapService = valueStreamMapService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalVsm.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws Exception {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = getCounter(request);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ValueStreamMapPresentationModel valueStreamMap = valueStreamMapService.getValueStreamMap(new CaseInsensitiveString(pipelineName), pipelineCounter, currentUsername(), result);
        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response, writer -> VSMRepresenter.toJSON(writer, valueStreamMap));
        }
        return renderHTTPOperationResult(result, request, response);
    }

    private Integer getCounter(Request request) {
        Integer value;
        try {
            value = Integer.valueOf(request.params("pipeline_counter"));
            if (value < 0) {
                throw new BadRequestException(BAD_PIPELINE_COUNTER_MSG);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException(BAD_PIPELINE_COUNTER_MSG);
        }
        return value;
    }

}
