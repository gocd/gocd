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
package com.thoughtworks.go.api.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.api.ControllerMethods;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.support.ServerStatusService;
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

import static com.thoughtworks.go.api.support.representers.ProcessListRepresenter.toJSON;
import static spark.Spark.get;
import static spark.Spark.path;

@Component
public class ApiSupportController implements SparkController, ControllerMethods, SparkSpringController {
    private ServerStatusService serverStatusService;

    private Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Autowired
    public ApiSupportController(ServerStatusService serverStatusService) {
        this.serverStatusService = serverStatusService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Support.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            get("", this::show);
            get(Routes.Support.PROCESS_LIST, this::processList);
        });
    }

    public String show(Request request, Response response) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Map<String, Object> information = serverStatusService.asJson(currentUsername(), result);
        response.type("application/json");
        if (result.isSuccessful()) {
            gson.toJson(information, response.raw().getWriter());
            return "";
        }

        return renderHTTPOperationResult(result, request, response);
    }

    public String processList(Request request, Response response) throws IOException {
        Collection<ProcessWrapper> processList = ProcessManager.getInstance().currentProcessListForDisplay();
        response.type("application/json");
        return writerForTopLevelObject(request, response, outputWriter -> toJSON(outputWriter, processList));
    }
}
