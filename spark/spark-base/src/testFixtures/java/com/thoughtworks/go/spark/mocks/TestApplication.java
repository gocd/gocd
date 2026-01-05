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
package com.thoughtworks.go.spark.mocks;

import com.thoughtworks.go.spark.RoutesHelper;
import com.thoughtworks.go.spark.SparkController;
import spark.Spark;
import spark.servlet.SparkApplication;

public class TestApplication implements SparkApplication {

    private final RoutesHelper routesHelper;

    public TestApplication(SparkController... sparkControllers) {
        routesHelper = new RoutesHelper(sparkControllers);
    }

    @Override
    public void init() {
        routesHelper.init();
    }

    @Override
    public void destroy() {
        Spark.stop();
        Spark.awaitStop();
    }
}
