/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.plugins.processor.serverinfo.v1;

import com.thoughtworks.go.config.ServerConfig;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageHandlerForServerInfoRequestProcessor1_0Test {
    @Test
    public void shouldSerializeServerConfigToJSON() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com:8154/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");

        MessageHandlerForServerInfoRequestProcessor1_0 processor = new MessageHandlerForServerInfoRequestProcessor1_0();

        assertThat(processor.serverInfoToJSON(serverConfig),
                is(format("{\"server_id\":\"%s\",\"site_url\":\"%s\",\"secure_site_url\":\"%s\"}",
                        serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl())));
    }
}
