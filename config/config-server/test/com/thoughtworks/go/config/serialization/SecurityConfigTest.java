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

package com.thoughtworks.go.config.serialization;

import java.io.ByteArrayOutputStream;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Test;

import static com.thoughtworks.go.config.SecurityConfigTest.admins;
import static com.thoughtworks.go.config.SecurityConfigTest.pwordFile;
import static com.thoughtworks.go.config.SecurityConfigTest.role;
import static com.thoughtworks.go.config.SecurityConfigTest.security;
import static com.thoughtworks.go.config.SecurityConfigTest.user;
import static org.mockito.Mockito.mock;

public class SecurityConfigTest {
    private MetricsProbeService metricsProbeService = new NoOpMetricsProbeService();

    @Test
    public void shouldAllowUnorderedRoleAndUserInXsd() throws Exception {
        CruiseConfig config = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService).loadConfigHolder(ConfigMigrator.migrate(
                ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS)).configForEdit;
        config.setServerConfig(new ServerConfig("dir", security(null, pwordFile(), admins(role("role2"), user("jez"), role("role1")))));
        new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService).write(config, new ByteArrayOutputStream(), false);
    }
}
