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

package com.thoughtworks.go.apiv2.apiinfo;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.apiinfo.representers.RouteEntryRepresenter;
import com.thoughtworks.go.spark.DeprecatedAPI;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.RouteEntry;
import com.thoughtworks.go.spark.spring.RouteInformationProvider;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

@Component
public class ApiInfoControllerV2 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final RouteInformationProvider provider;

    @Autowired
    public ApiInfoControllerV2(ApiAuthenticationHelper apiAuthenticationHelper,
                               RouteInformationProvider provider) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.provider = provider;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ApiInfo.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<RouteEntry> filteredRoutes = provider.getRoutes().stream()
            .filter(RouteEntry::isAPI)
            .sorted(Comparator.comparing(RouteEntry::getPath))
            .collect(Collectors.toList());

        return writerForTopLevelArray(request, response, writer -> RouteEntryRepresenter.toJSON(writer, filteredRoutes));
    }
}
