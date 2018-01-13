/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.api;

import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static spark.Spark.afterAfter;
import static spark.Spark.before;

public class RoutesHelper {
    private static final String TIMER_START = RuntimeHeaderEmitter.class.getName();

    private List<SparkController> controllers;
    private List<BaseController> sparkControllers;

    public RoutesHelper(SparkController... controllers) {
        this(controllers, null);
    }

    public RoutesHelper(BaseController... sparkControllers) {
        this(null, sparkControllers);
    }

    private RoutesHelper(SparkController[] controllers, BaseController[] baseControllers) {
        this.controllers = controllers == null ? Collections.emptyList() : Arrays.asList(controllers);
        this.sparkControllers = baseControllers == null ? Collections.emptyList() : Arrays.asList(baseControllers);
    }

    public void init() {
        before("/*", (request, response) -> request.attribute(TIMER_START, new RuntimeHeaderEmitter(request, response)));
        before("/*", (request, response) -> response.header("Cache-Control", "max-age=0, private, must-revalidate"));

        controllers.forEach(SparkController::setupRoutes);
        sparkControllers.forEach(BaseController::setupRoutes);

        afterAfter("/*", (request, response) -> request.<RuntimeHeaderEmitter>attribute(TIMER_START).render());
    }


    private class RuntimeHeaderEmitter {
        private final Request request;
        private final Response response;
        private final long timerStart;

        public RuntimeHeaderEmitter(Request request, Response response) {
            this.timerStart = System.currentTimeMillis();
            this.request = request;
            this.response = response;
        }

        public void render() {
            if (!response.raw().isCommitted()) {
                response.header("X-Runtime", String.valueOf(System.currentTimeMillis() - timerStart));
            }
        }
    }
}
