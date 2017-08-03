/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.support;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ServerStatusService {
    private SecurityService securityService;
    private List<ServerInfoProvider> providers = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(ServerStatusService.class);

    @Autowired
    public ServerStatusService(SecurityService securityService, ServerInfoProvider... providerArray) throws IOException {
        this.securityService = securityService;

        providers.addAll(Arrays.asList(providerArray));
        providers.sort((oneProvider, anotherProvider) -> Double.compare(oneProvider.priority(), anotherProvider.priority()));
        Runtime.getRuntime().addShutdownHook(logServerInfo());
    }

    private Thread logServerInfo() {
        return new Thread(() -> LOGGER.info(new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(serverInfoAsJson())));
    }

    public Map<String, Object> asJson(Username username, LocalizedOperationResult result) {
        if (!securityService.isUserAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"), HealthStateType.unauthorised());
            return null;
        }

        return serverInfoAsJson();

    }

    private Map<String, Object> serverInfoAsJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Timestamp", DateUtils.formatISO8601(new Date()));

        for (ServerInfoProvider provider : providers) {
            try {
                json.put(provider.name(), provider.asJson());
            } catch (Exception e) {
                json.put(provider.getClass().getCanonicalName(), String.format("Provider %s threw an exception: %s", provider.getClass(), e.getMessage()));
                LOGGER.warn("An API support page provider failed.", e);
            }
        }
        return json;
    }
}
