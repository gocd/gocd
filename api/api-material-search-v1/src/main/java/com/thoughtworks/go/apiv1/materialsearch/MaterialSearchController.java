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
package com.thoughtworks.go.apiv1.materialsearch;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.materialsearch.representers.MatchedRevisionRepresenter;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.server.service.MaterialService;
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
public class MaterialSearchController extends ApiController implements SparkSpringController {

    private final MaterialService materialService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public MaterialSearchController(MaterialService materialService, ApiAuthenticationHelper apiAuthenticationHelper) {
        super(ApiVersion.v1);
        this.materialService = materialService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialSearch.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);

            get("", mimeType, this::search);
            head("", mimeType, this::search);
        });
    }

    public String search(Request request, Response response) throws IOException {
        String pipelineName = request.queryParams("pipeline_name");
        String fingerprint = request.queryParams("fingerprint");
        String searchText = request.queryParamOrDefault("search_text", "");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MatchedRevision> matchedRevisions = materialService.searchRevisions(pipelineName, fingerprint, searchText, currentUsername(), result);
        if (result.isSuccessful()) {
            return writerForTopLevelArray(request, response, outputListWriter -> MatchedRevisionRepresenter.toJSON(outputListWriter, matchedRevisions));
        } else {
            return renderHTTPOperationResult(result, request, response);
        }
    }
}
