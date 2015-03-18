/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class ServerStatusService {
    private SecurityService securityService;
    private List<ServerInfoProvider> providers = new ArrayList<ServerInfoProvider>();
    private static final Logger LOGGER = Logger.getLogger(ServerStatusService.class);

    @Autowired
    public ServerStatusService(SecurityService securityService, ServerInfoProvider... providerArray) throws IOException {
        this.securityService = securityService;

        providers.addAll(Arrays.asList(providerArray));
        Collections.sort(providers, new Comparator<ServerInfoProvider>() {
            @Override
            public int compare(ServerInfoProvider oneProvider, ServerInfoProvider anotherProvider) {
                return Double.compare(oneProvider.priority(), anotherProvider.priority());
            }
        });
    }

    public String captureServerInfo(Username username, LocalizedOperationResult result) throws IOException {
        if (!securityService.isUserAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"), HealthStateType.unauthorised());
            return null;
        }
        return serverInfo();
    }

    private void populateServerInfo(File serverInfoFile) throws IOException {
        FileUtils.writeStringToFile(serverInfoFile, serverInfo());
    }

    private String serverInfo() {
        InformationStringBuilder infoCollector = new InformationStringBuilder(new StringBuilder());

        infoCollector.append("\n\nTimestamp:\n==========\n");
        infoCollector.append(new Date().toString()).append("\n");
        for (ServerInfoProvider provider : providers) {
            try {
                provider.appendInformation(infoCollector);
            } catch (Exception e) {
                infoCollector.append(String.format("Provider %s threw an exception: %s", provider.getClass(), e.getMessage()));
                LOGGER.warn("An API support page provider failed.", e);
            }
        }

        return infoCollector.value();
    }

}