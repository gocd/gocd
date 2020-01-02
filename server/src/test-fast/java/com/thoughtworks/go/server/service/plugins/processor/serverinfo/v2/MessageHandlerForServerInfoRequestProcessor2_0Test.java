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
package com.thoughtworks.go.server.service.plugins.processor.serverinfo.v2;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.ServerConfig;
import org.junit.Test;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class MessageHandlerForServerInfoRequestProcessor2_0Test {
    @Test
    public void shouldSerializeServerConfigToJSON() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com:8154/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");
        CurrentGoCDVersion goCDVersion = CurrentGoCDVersion.getInstance();

        MessageHandlerForServerInfoRequestProcessor2_0 processor = new MessageHandlerForServerInfoRequestProcessor2_0();

        String expectedJsonStr = format("{\"server_id\":\"%s\"," +
                        "\"site_url\":\"%s\"," +
                        "\"secure_site_url\":\"%s\"," +
                        "\"go_version\":\"%s\"," +
                        "\"dist_version\":\"%s\"," +
                        "\"git_revision\":\"%s\"" +
                        "}",
                serverConfig.getServerId(),
                serverConfig.getSiteUrl().getUrl(),
                serverConfig.getSecureSiteUrl().getUrl(),
                goCDVersion.goVersion(),
                goCDVersion.distVersion(),
                goCDVersion.gitRevision()
        );

        assertThatJson(expectedJsonStr).isEqualTo(processor.serverInfoToJSON(serverConfig));
    }
}
