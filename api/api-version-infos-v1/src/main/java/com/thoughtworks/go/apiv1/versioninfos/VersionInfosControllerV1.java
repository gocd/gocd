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
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.versioninfos.representers.VersionInfoRepresenter;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

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
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get(Routes.VersionInfos.STALE, this::stale);
            get(Routes.VersionInfos.LATEST_VERSION, this::latestVersion);
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
}
