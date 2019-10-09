/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.compare;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.compare.representers.ComparisonRepresenter;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.server.service.ChangesetService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

@Component
public class CompareControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ChangesetService changesetService;

    @Autowired
    public CompareControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, ChangesetService changesetService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.changesetService = changesetService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CompareAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this::verifyContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer fromCounter = Integer.valueOf(request.params("from_counter"));
        Integer toCounter = Integer.valueOf(request.params("to_counter"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        List<MaterialRevision> materialRevisions = changesetService.revisionsBetween(pipelineName, fromCounter, toCounter, currentUsername(), result, false);

        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response, outputWriter -> ComparisonRepresenter.toJSON(outputWriter, pipelineName, fromCounter, toCounter, materialRevisions));
        } else {
            return renderHTTPOperationResult(result, request, response);
        }
    }
}
