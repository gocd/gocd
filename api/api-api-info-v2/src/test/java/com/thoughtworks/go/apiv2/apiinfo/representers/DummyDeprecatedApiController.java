/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.apiv2.apiinfo.representers;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.spark.DeprecatedAPI;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

@DeprecatedAPI(deprecatedApiVersion = ApiVersion.v1, successorApiVersion = ApiVersion.v2, deprecatedIn = "20.2.0", removalIn = "20.5.0", entityName = "Do Nothing")
public class DummyDeprecatedApiController implements SparkSpringController {
    @Override
    public void setupRoutes() {
    }

    public String doNothing(Request request, Response response) throws Exception {
        return "";
    }

    public RouteImpl doNothingRouteImpl() {
        return RouteImpl.create("", "", this::doNothing);
    }
}
