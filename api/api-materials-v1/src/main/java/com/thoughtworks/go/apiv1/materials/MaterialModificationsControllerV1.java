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
package com.thoughtworks.go.apiv1.materials;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.materials.representers.ModificationsRepresenter;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class MaterialModificationsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;
    private final MaterialService materialService;

    @Autowired
    public MaterialModificationsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService,
                                             MaterialService materialService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
        this.materialService = materialService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialModifications.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            get("", mimeType, this::modifications);
            get(Routes.MaterialModifications.OFFSET, mimeType, this::modifications);
        });
    }

    public String modifications(Request req, Response res) throws IOException {
        String fingerprint = req.params("fingerprint");
        Integer offset = req.params("offset") == null ? null : Integer.parseInt(req.params("offset"));
        HttpOperationResult result = new HttpOperationResult();
        MaterialConfig materialConfig = materialConfigService.getMaterialConfig(currentUsernameString(), fingerprint, result);
        if (result.canContinue()) {
            Long modificationsCount = materialService.getTotalModificationsFor(materialConfig);
            Pagination pagination = Pagination.pageStartingAt(offset, modificationsCount.intValue(), 10);
            Modifications modifications = materialService.getModificationsFor(materialConfig, pagination);
            return writerForTopLevelObject(req, res, writer -> ModificationsRepresenter.toJSON(writer, modifications, pagination, fingerprint));
        } else {
            return renderHTTPOperationResult(result, req, res);
        }
    }
}
