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

import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.web.JsonRenderer.render;

/**
 * @understands Dumping all the server health messages
 */
@Component
public class ServerHealthInformationProvider implements ServerInfoProvider {

    private ServerHealthService service;

    @Override
    public double priority() {
        return 4.0;
    }

    @Autowired
    public ServerHealthInformationProvider(ServerHealthService service) {
        this.service = service;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection("Server health information");
        ServerHealthStates allLogs = service.getAllLogs();
        infoCollector.append(String.format("Server Health Messages: [%d messages]\n", allLogs.size()));
        for (ServerHealthState log : allLogs) {
            infoCollector.append(String.format("%s\n", render(log.asJson())));
        }
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        ServerHealthStates allLogs = service.getAllLogs();
        json.put("Messages Count", allLogs.size());

        ArrayList<Map<String, String>> messages = new ArrayList<>();
        for (ServerHealthState log : allLogs) {
            messages.add(log.asJson());
        }
        json.put("Messages", messages);
        return json;
    }

    @Override
    public String name() {
        return "Server Health Information";
    }
}
