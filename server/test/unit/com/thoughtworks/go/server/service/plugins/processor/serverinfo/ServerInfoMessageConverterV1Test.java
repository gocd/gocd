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

package com.thoughtworks.go.server.service.plugins.processor.serverinfo;

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Test;

public class ServerInfoMessageConverterV1Test {

    @Test
    public void shouldReturnServerIdInCorrectJSONFormat() throws Exception {

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com:8154/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");


        DefaultGoApiResponse response = new ServerInfoMessageConverterV1().getServerInfo(serverConfig);

        JsonAssert.assertJsonEquals("{\n" +
                "  \"server_id\": \"" + serverConfig.getServerId() + "\",\n" +
                "  \"site_url\": \"" + serverConfig.getSiteUrl().getUrl() + "\",\n" +
                "  \"secure_site_url\": \"" + serverConfig.getSecureSiteUrl().getUrl() + "\"\n" +
                "}", response.responseBody());
    }
}