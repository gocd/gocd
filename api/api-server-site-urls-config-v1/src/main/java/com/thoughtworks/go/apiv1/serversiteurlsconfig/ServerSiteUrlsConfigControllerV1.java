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

package com.thoughtworks.go.apiv1.serversiteurlsconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.serversiteurlsconfig.representers.ServerSiteUrlsConfigRepresenter;
import com.thoughtworks.go.config.SiteUrls;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class ServerSiteUrlsConfigControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private ServerConfigService serverConfigService;

    @Autowired
    public ServerSiteUrlsConfigControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ServerSiteUrlsConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::createOrUpdate);
            put("", mimeType, this::createOrUpdate);

            exception(RuntimeException.class, (RuntimeException exception, Request request, Response response) -> {
                response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                response.body(MessageJson.create(exception.getMessage()));
            });
        });
    }

    String createOrUpdate(Request request, Response response) throws IOException {
        SiteUrls siteUrls = buildEntityFromRequestBody(request);
        try {
            serverConfigService.createOrUpdateServerSiteUrls(siteUrls);
            return writerForTopLevelObject(request, response, writer -> ServerSiteUrlsConfigRepresenter.toJSON(writer, siteUrls));
        } catch (GoConfigInvalidException e) {
            return writerForTopLevelObject(request, response, writer -> {
                response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                writer.add("message", e.getMessage());
                ServerSiteUrlsConfigRepresenter.toJSON(writer, siteUrls);
            });
        }
    }

    String index(Request request, Response response) throws IOException {
        SiteUrls serverSiteUrls = serverConfigService.getServerSiteUrls();
        return writerForTopLevelObject(request, response, writer -> ServerSiteUrlsConfigRepresenter.toJSON(writer, serverSiteUrls));
    }

    private SiteUrls buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ServerSiteUrlsConfigRepresenter.fromJson(jsonReader);
    }
}
