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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;

import java.util.HashMap;
import java.util.Map;

public class AppServerStub extends AppServer {

    public final Map<String, Object> calls = new HashMap<>();
    public final Map<String, String> initParams = new HashMap<>();

    public AppServerStub(SystemEnvironment systemEnvironment) {
        super(systemEnvironment);
    }

    @Override
    void setSessionConfig() {
        calls.put("setSessionCookieConfig", "something");
    }

    @Override
    void setInitParameter(String name, String value) {
        initParams.put(name, value);
    }

    @Override
    Throwable getUnavailableException() {
        calls.put("getUnavailableException", true);
        return null;
    }

    @Override
    void configure() {
        calls.put("configure", true);
    }

    @Override
    void start() {
        calls.put("start", true);
    }

    @Override
    void stop() {
        calls.put("stop", true);

    }

    @Override
    public boolean hasStarted() {
        calls.put("hasStarted", true);
        return true;
    }
}
