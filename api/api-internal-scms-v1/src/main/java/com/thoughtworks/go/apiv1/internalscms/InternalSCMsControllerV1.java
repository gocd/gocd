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

package com.thoughtworks.go.apiv1.internalscms;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.internalscms.representers.SCMRepresenter;
import com.thoughtworks.go.apiv1.internalscms.representers.VerifyConnectionResultRepresenter;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class InternalSCMsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private PluggableScmService pluggableScmService;

    @Autowired
    public InternalSCMsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                    PluggableScmService pluggableScmService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pluggableScmService = pluggableScmService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.SCM.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.SCM.VERIFY_CONNECTION, mimeType, this::setContentType);
            before(Routes.SCM.VERIFY_CONNECTION, mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            post(Routes.SCM.VERIFY_CONNECTION, mimeType, this::verifyConnection);
        });
    }

    public String verifyConnection(Request request, Response response) throws IOException {
        SCM scm = buildEntityFromRequestBody(request);
        HttpLocalizedOperationResult checkConnectionResult = pluggableScmService.checkConnection(scm);

        response.status(checkConnectionResult.httpCode());
        return writerForTopLevelObject(request, response, writer -> VerifyConnectionResultRepresenter.toJSON(writer, scm, checkConnectionResult));
    }

    public SCM buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return SCMRepresenter.fromJSON(jsonReader);
    }
}
