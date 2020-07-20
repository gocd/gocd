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

package com.thoughtworks.go.apiv1.versioninfos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.versioninfos.models.GoLatestVersion;
import com.thoughtworks.go.apiv1.versioninfos.representers.GoLatestVersionRepresenter;
import com.thoughtworks.go.apiv1.versioninfos.representers.VersionInfoRepresenter;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class VersionInfosControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private VersionInfoService versionInfoService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public VersionInfosControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, VersionInfoService versionInfoService, SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.versionInfoService = versionInfoService;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return Routes.VersionInfos.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);

            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get(Routes.VersionInfos.STALE, mimeType, this::stale);
            get(Routes.VersionInfos.LATEST_VERSION, mimeType, this::latestVersion);
            patch(Routes.VersionInfos.GO_SERVER, mimeType, this::updateGoServerInfo);
        });
    }

    public String stale(Request request, Response response) throws Exception {
        VersionInfo staleVersionInfo = versionInfoService.getStaleVersionInfo();
        return writerForTopLevelObject(request, response, writer -> VersionInfoRepresenter.toJSON(writer, staleVersionInfo, systemEnvironment));
    }

    public String latestVersion(Request request, Response response) throws Exception {
        String goUpdate = versionInfoService.getGoUpdate();
        return writerForTopLevelObject(request, response, writer -> {
            if (StringUtils.isNotBlank(goUpdate)) {
                writer.add("latest_version", goUpdate);
            }
        });
    }

    public String updateGoServerInfo(Request request, Response response) throws IOException {
        GoLatestVersion goLatestVersion = buildEntityFromRequest(request);
        goLatestVersion.setSystemEnvironment(systemEnvironment);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (!goLatestVersion.isValid()) {
            log.error("[Go Update Check] Latest version update failed, version information from update server tampered.");
            result.badRequest("Message tampered, cannot process.");
            return renderHTTPOperationResult(result, request, response);
        }

        VersionInfo versionInfo = versionInfoService.updateServerLatestVersion(goLatestVersion.latestVersion(), result);

        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response, writer -> VersionInfoRepresenter.toJSON(writer, versionInfo, systemEnvironment));
        } else {
            return renderHTTPOperationResult(result, request, response);
        }
    }

    private GoLatestVersion buildEntityFromRequest(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return GoLatestVersionRepresenter.fromJSON(jsonReader);
    }
}
