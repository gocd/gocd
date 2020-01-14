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
package com.thoughtworks.go.apiv2.notificationfilter;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.notificationfilter.representers.NotificationFilterRepresenter;
import com.thoughtworks.go.apiv2.notificationfilter.representers.NotificationFiltersRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.exception.UncheckedValidationException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class NotificationFilterControllerV2 extends ApiController implements SparkSpringController, CrudController<NotificationFilter> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final UserService userService;
    private final GoConfigService goConfigService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public NotificationFilterControllerV2(ApiAuthenticationHelper apiAuthenticationHelper,
                                          UserService userService,
                                          GoConfigService goConfigService,
                                          EntityHashingService entityHashingService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.userService = userService;
        this.goConfigService = goConfigService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.NotificationFilterAPI.API_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("", mimeType, this::canConfigureNotificationFilters);

            before("/*", mimeType, this::verifyContentType);
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this::canConfigureNotificationFilters);

            get("", mimeType, this::index);
            get(Routes.NotificationFilterAPI.ID, mimeType, this::show);
            post("", mimeType, this::createNotificationFilter);
            patch(Routes.NotificationFilterAPI.ID, mimeType, this::updateFilter);
            delete(Routes.NotificationFilterAPI.ID, mimeType, this::deleteFilter);
        });
    }

    public String index(Request request, Response response) throws IOException {
        User user = userService.findUserByName(currentUsernameString());
        List<NotificationFilter> notificationFilters = user.getNotificationFilters();

        return writerForTopLevelObject(request, response, writer -> NotificationFiltersRepresenter.toJSON(writer, notificationFilters));
    }

    public String show(Request request, Response response) throws IOException {
        NotificationFilter notificationFilter = doFetchEntityFromConfig(request.params("id"));

        if (notificationFilter == null) {
            throw new RecordNotFoundException(EntityType.NotificationFilter, parseIdToLong(request.params("id")));
        }

        return writerForTopLevelObject(request, response, writer -> NotificationFilterRepresenter.toJSON(writer, notificationFilter));
    }

    public String createNotificationFilter(Request request, Response response) throws IOException {
        NotificationFilter notificationFilter = buildEntityFromRequestBody(request);

        try {
            userService.addNotificationFilter(currentUserId(request), notificationFilter);
            setEtagHeader(response, etagFor(notificationFilter));
        } catch (UnprocessableEntityException e) {
            response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }

        return writerForTopLevelObject(request, response, writer -> NotificationFilterRepresenter.toJSON(writer, notificationFilter));
    }

    public String updateFilter(Request request, Response response) throws IOException {
        long id = parseIdToLong(request.params("id"));
        NotificationFilter notificationFilter = buildEntityFromRequestBody(request);
        notificationFilter.setId(id);

        try {
            userService.updateNotificationFilter(currentUserId(request), notificationFilter);
            setEtagHeader(response, etagFor(notificationFilter));
        } catch (UncheckedValidationException | UnprocessableEntityException e) {
            response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
        return writerForTopLevelObject(request, response, writer -> NotificationFilterRepresenter.toJSON(writer, notificationFilter));
    }

    public String deleteFilter(Request request, Response response) {
        long id = parseIdToLong(request.params("id"));
        userService.removeNotificationFilter(currentUserId(request), id);
        return renderMessage(response, 200, "Notification filter is successfully deleted!");
    }

    @Override
    public String etagFor(NotificationFilter entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NotificationFilter;
    }

    @Override
    public NotificationFilter doFetchEntityFromConfig(String id) {
        return userService.findUserByName(currentUsernameString())
            .getNotificationFilters()
            .stream()
            .filter(filter -> parseIdToLong(id) == filter.getId())
            .findFirst()
            .orElse(null);
    }

    @Override
    public NotificationFilter buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return NotificationFilterRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(NotificationFilter notificationFilter) {
        return outputWriter -> NotificationFilterRepresenter.toJSON(outputWriter, notificationFilter);
    }

    private long parseIdToLong(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("The notification id should be an integer.");
        }
    }

    private void canConfigureNotificationFilters(Request request, Response response) {
        if (!goConfigService.isSmtpEnabled()) {
            throw new BadRequestException("SMTP settings are currently not configured. Ask your administrator to configure SMTP settings.");
        }
    }
}
