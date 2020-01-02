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
package com.thoughtworks.go.spark.spring;

import com.thoughtworks.go.spark.RerouteLatestApis;
import com.thoughtworks.go.spark.RoutesHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.globalstate.ServletFlag;
import spark.servlet.SparkApplication;

@Component
public class Application implements SparkApplication {

    @Autowired
    public Application(RerouteLatestApis rerouteLatestApis, SparkSpringController... controllers) {
        ServletFlag.runFromServlet();
        RoutesHelper routesHelper = new RoutesHelper(controllers);
        routesHelper.init();
        rerouteLatestApis.registerLatest();
    }

    @Override
    public void init() {

    }

}
