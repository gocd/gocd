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

package com.thoughtworks.go.apiv1.materialsearch;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.materialsearch.representers.MatchedRevisionRepresenter;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.util.List;

import static com.thoughtworks.go.spark.RequestContext.requestContext;
import static spark.Spark.*;

public class MaterialSearchControllerDelegate extends ApiController {

    private final MaterialService materialService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Localizer localizer;

    public MaterialSearchControllerDelegate(MaterialService materialService, ApiAuthenticationHelper apiAuthenticationHelper, Localizer localizer) {
        super(ApiVersion.v1);
        this.materialService = materialService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.localizer = localizer;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialSearch.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", this::setContentType);
            before("/*", this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before("/*", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);

            get("", this::search, GsonTransformer.getInstance());
            head("", this::search, GsonTransformer.getInstance());
        });
    }

    public Object search(Request request, Response response) {
        String pipelineName = request.queryParams("pipeline_name");
        String fingerprint = request.queryParams("fingerprint");
        String searchText = request.queryParamOrDefault("search_text", "");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MatchedRevision> matchedRevisions = materialService.searchRevisions(pipelineName, fingerprint, searchText, currentUsername(), result);
        if(result.isSuccessful()) {
            return MatchedRevisionRepresenter.toJSON(matchedRevisions, requestContext(request));
        }
        return renderHTTPOperationResult(result, response, localizer);
    }
}
