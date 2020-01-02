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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;

import java.util.HashMap;

public class AppServerStub extends AppServer {

    public HashMap<String, Object> calls = new HashMap<>();
    public HashMap<String, String> initparams = new HashMap<>();

    public AppServerStub(SystemEnvironment systemEnvironment, String password) {
        super(systemEnvironment, password);
    }

    @Override
    void addExtraJarsToClasspath(String extraClasspath) {
        calls.put("addExtraJarsToClasspath", extraClasspath);
    }

    @Override
    void setSessionConfig() {
        calls.put("setSessionCookieConfig", "something");
    }

    @Override
    void setInitParameter(String name, String value) {
        initparams.put(name, value);
    }

    @Override
    Throwable getUnavailableException() {
        calls.put("getUnavailableException", true);
        return null;
    }

    @Override
    void configure() throws Exception {
        calls.put("configure", true);
    }

    @Override
    void start() throws Exception {
        calls.put("start", true);
    }

    @Override
    void stop() throws Exception {
        calls.put("stop", true);

    }

    @Override
    public boolean hasStarted() {
        calls.put("hasStarted", true);
        return true;
    }
}
