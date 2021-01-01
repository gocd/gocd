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
package com.thoughtworks.go.apiv1.mailserver;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.mailserver.representers.MailServerRepresenter;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.CreateOrUpdateUpdateMailHostCommand;
import com.thoughtworks.go.config.update.DeleteMailHostCommand;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class MailServerControllerV1 extends ApiController implements SparkSpringController, CrudController<MailHost> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final GoConfigService goConfigService;
    private final ServerConfigService serverConfigService;

    @Autowired
    public MailServerControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService, ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.goConfigService = goConfigService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MailServer.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::show);
            post("", mimeType, this::createOrUpdate);
            put("", mimeType, this::createOrUpdate);

            delete("", mimeType, this::deleteMailConfig);

            post(Routes.MailServer.TEST_EMAIL, mimeType, this::sendTestEmail);
        });
    }

    public String show(Request request, Response response) throws IOException {
        final MailHost mailhost = fetchEntityFromConfig();

        return writerForTopLevelObject(request, response, jsonWriter(mailhost));
    }

    public String createOrUpdate(Request request, Response response) throws IOException {
        MailHost mailHost = buildEntityFromRequestBody(request);

        try {
            goConfigService.updateConfig(new CreateOrUpdateUpdateMailHostCommand(mailHost), currentUsername());
        } catch (GoConfigInvalidException e) {
            response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            return MessageJson.create(e.getMessage(), jsonWriter(mailHost));
        }
        return show(request, response);
    }

    public String deleteMailConfig(Request request, Response response) {
        fetchEntityFromConfig();

        goConfigService.updateConfig(new DeleteMailHostCommand(), currentUsername());

        return renderMessage(response, 200, EntityType.SMTP.deleteSuccessful());
    }

    public String sendTestEmail(Request request, Response response) throws IOException {
        MailHost mailHost = buildEntityFromRequestBody(request);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        serverConfigService.sendTestMail(mailHost, result);
        return renderHTTPOperationResult(result, request, response);
    }

    @Override
    public String etagFor(MailHost entityFromServer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SMTP;
    }

    @Override
    public MailHost doFetchEntityFromConfig() {
        return goConfigService.getMailHost();
    }

    @Override
    public MailHost buildEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return MailServerRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(MailHost mailHost) {
        return writer -> MailServerRepresenter.toJSON(writer, mailHost);
    }
}
