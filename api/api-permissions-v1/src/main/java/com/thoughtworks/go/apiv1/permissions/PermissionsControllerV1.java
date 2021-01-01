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

package com.thoughtworks.go.apiv1.permissions;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.permissions.representers.PermissionsRepresenter;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.server.service.permissions.PermissionsService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

@Component
public class PermissionsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private PermissionsService permissionsService;

    @Autowired
    public PermissionsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, PermissionsService permissionsService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.permissionsService = permissionsService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Permissions.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<String> permissibleEntities = permissionsService.allEntitiesSupportsPermission();
        List<String> requestedTypes = permissibleEntities;

        String type = request.queryParams("type");
        if (StringUtils.isNotBlank(type)) {
            requestedTypes = Arrays.stream(type.split(",")).collect(Collectors.toList());
            validateRequestedTypes(requestedTypes, permissibleEntities);
        }

        Map<String, Object> permissions = permissionsService.getPermissions(requestedTypes);
        return writerForTopLevelObject(request, response, outputWriter -> PermissionsRepresenter.toJSON(outputWriter, permissions));
    }

    private void validateRequestedTypes(List<String> requestedTypes, List<String> permissibleEntities) {
        requestedTypes.forEach(type -> {
            if (StringUtils.isNotBlank(type.trim()) && !permissibleEntities.contains(type)) {
                throw new UnprocessableEntityException(String.format("Invalid permission type '%s'. It has to be one of '%s'.", type, String.join(", ", permissibleEntities)));
            }
        });
    }
}
