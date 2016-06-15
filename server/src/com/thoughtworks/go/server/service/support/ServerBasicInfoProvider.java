/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.util.ServerVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ServerBasicInfoProvider implements ServerInfoProvider {
    private ServerVersion serverVersion;

    @Autowired
    public ServerBasicInfoProvider(ServerVersion serverVersion){
        this.serverVersion = serverVersion;
    }

    @Override
    public double priority() {
        return 3.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection("Go Server Version");
        infoCollector.append(serverVersion.version());
        infoCollector.append("\n");
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Version", serverVersion.version());
        return json;
    }

    @Override
    public String name() {
        return "Go Server Information";
    }
}
