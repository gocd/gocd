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

package com.thoughtworks.go.apiv1.webhook;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class WebhookControllerV1 extends ApiController implements SparkSpringController, CrudController<Webhook> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;

    @Autowired
    public WebhookControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Webhook.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            // uncomment the line below to set the content type on the base path
            // before("", mimeType, this::setContentType);
            // uncomment the line below to set the content type on nested routes
            // before("/*", mimeType, this::setContentType);

            // uncomment for the `index` action
            // get("", mimeType, this::index);

            // change the line below to enable appropriate security
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            // to be implemented
        });
    }

    // public String index(Request request, Response response) throws IOException {
    //    Webhook webhook = fetchEntityFromConfig(request.params(":id"));
    //    return writerForTopLevelObject(request, response, outputWriter -> WebhooksRepresenter.toJSON(outputWriter, webhook));
    // }


    @Override
    public String etagFor(Webhook entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.Webhook;
    }

    @Override
    public Webhook doFetchEntityFromConfig(String name) {
        return someService.getEntity(name);
    }

    @Override
    public Webhook buildEntityFromRequestBody(Request req) {
      JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
      return WebhookRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(Webhook webhook) {
        return outputWriter -> WebhookRepresenter.toJSON(outputWriter, webhook);
    }
}
