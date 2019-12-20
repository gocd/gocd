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

package com.thoughtworks.go.apiv1.processlist;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.ProcessWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Collection;

import static com.thoughtworks.go.apiv1.processlist.representers.ProcessListRepresenter.toJSON;
import static spark.Spark.*;

@Component
public class ProcessListControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public ProcessListControllerV1(ApiAuthenticationHelper apiAuthenticationHelper) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ProcessListAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        Collection<ProcessWrapper> processList = ProcessManager.getInstance().currentProcessListForDisplay();
        return writerForTopLevelObject(request, response, outputWriter -> toJSON(outputWriter, processList));
    }
}
