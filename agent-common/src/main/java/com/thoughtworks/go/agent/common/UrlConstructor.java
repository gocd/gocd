/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.common;

import com.thoughtworks.go.agent.ServerUrlGenerator;

import java.net.MalformedURLException;

public class UrlConstructor implements ServerUrlGenerator {

    private final String serverUrl;

    public UrlConstructor(String serverUrl) throws MalformedURLException {
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        this.serverUrl = serverUrl;
    }

    public String serverUrlFor(String subPath) {
        if (subPath == null || subPath.trim().length() == 0) {
            return serverUrl;
        }
        return serverUrl + "/" + subPath;
    }
}
