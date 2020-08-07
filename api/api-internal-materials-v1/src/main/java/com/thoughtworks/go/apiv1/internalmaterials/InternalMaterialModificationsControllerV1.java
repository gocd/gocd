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

package com.thoughtworks.go.apiv1.internalmaterials;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.internalmaterials.representers.ModificationsRepresenter;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.*;

@Component
public class InternalMaterialModificationsControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;
    private final MaterialService materialService;

    public InternalMaterialModificationsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService, MaterialService materialService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
        this.materialService = materialService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalMaterialConfig.INTERNAL_BASE_MODIFICATIONS;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::modifications);
        });
    }

    public String modifications(Request request, Response response) throws Exception {
        String fingerprint = request.params("fingerprint");
        Long after = afterCursor(request);
        Long before = beforeCursor(request);
        Integer pageSize = getPageSize(request);
        String pattern = request.queryParamOrDefault("pattern", "");

        HttpOperationResult result = new HttpOperationResult();
        MaterialConfig materialConfig = materialConfigService.getMaterialConfig(currentUsernameString(), fingerprint, result);

        if (!result.canContinue()) {
            return renderHTTPOperationResult(result, request, response);
        }

        List<Modification> modifications = materialService.getModificationsFor(materialConfig, pattern, after, before, pageSize);
        PipelineRunIdInfo info = materialService.getLatestAndOldestModification(materialConfig, pattern);

        return writerForTopLevelObject(request, response, writer -> ModificationsRepresenter.toJSON(writer, modifications, info, materialConfig.getFingerprint(), pattern));

    }
}
