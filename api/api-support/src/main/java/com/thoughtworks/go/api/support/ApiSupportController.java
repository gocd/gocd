/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.api.support;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.api.ControllerMethods;
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper;
import com.thoughtworks.go.api.support.representers.ProcessListRepresenter;
import com.thoughtworks.go.server.service.support.ServerStatusService;
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.ProcessWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static spark.Spark.*;

@Component
public class ApiSupportController implements SparkController, ControllerMethods, SparkSpringController {
    static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(excludeLocks())
        .serializeNulls()
        .disableHtmlEscaping()
        .create();

    private final ApiAuthorizationHelper apiAuthorizationHelper;
    private final ServerStatusService serverStatusService;

    @Autowired
    public ApiSupportController(ApiAuthorizationHelper apiAuthorizationHelper, ServerStatusService serverStatusService) {
        this.apiAuthorizationHelper = apiAuthorizationHelper;
        this.serverStatusService = serverStatusService;
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String controllerBasePath() {
        return Routes.Support.BASE;
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        path(controllerBasePath(), () -> {
            before("", this::setContentType);
            before("/*", this::setContentType);

            before("", this.apiAuthorizationHelper::checkAdminUserAnd403);
            before("/*", this.apiAuthorizationHelper::checkAdminUserAnd403);

            get("", this::show);
            get(Routes.Support.PROCESS_LIST, this::processList);
        });
    }

    public String show(Request request, Response response) throws IOException {
        Map<String, Object> information = serverStatusService.asJsonCompatibleMap();
        GSON.toJson(information, response.raw().getWriter());
        return NOTHING;
    }

    public String processList(Request request, Response response) throws IOException {
        Collection<ProcessWrapper> processList = ProcessManager.getInstance().currentProcessListForDisplay();
        return writerForTopLevelObject(request, response, outputWriter -> ProcessListRepresenter.toJSON(outputWriter, processList));
    }

    private static ExclusionStrategy excludeLocks() {
        return new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                // Don't try to serialize locks which might be inside Hibernate clsses
                return Lock.class.isAssignableFrom(clazz);
            }
        };
    }

    public void setContentType(Request request, Response response) {
        response.raw().setCharacterEncoding("utf-8");
        response.type(getMimeType());
    }
}
