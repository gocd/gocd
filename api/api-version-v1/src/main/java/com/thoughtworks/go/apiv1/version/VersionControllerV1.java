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
package com.thoughtworks.go.apiv1.version;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;

import static spark.Spark.path;

@Component
public class VersionControllerV1 extends ApiController implements SparkSpringController {

    @Autowired
    public VersionControllerV1() {
        super(ApiVersion.v1);
    }

    @Override
    public String controllerBasePath() {
        return Routes.Version.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            Spark.get("", mimeType, this::show);
        });
    }

    public String show(Request req, Response res) throws IOException {
        return writerForTopLevelObject(req, res, writer -> VersionRepresenter.toJSON(writer, CurrentGoCDVersion.getInstance()));
    }
}
