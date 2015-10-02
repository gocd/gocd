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
package com.thoughtworks.go.config;

import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG;
import static org.mockito.Mockito.mock;

public class CachedFileGoConfigTest extends CachedGoConfigTestBase {
    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(CONFIG);
        SystemEnvironment env = new SystemEnvironment();
        ConfigRepository configRepository = new ConfigRepository(env);
        configRepository.initialize();
        dataSource = new GoFileConfigDataSource(new DoNotUpgrade(), configRepository, env, new TimeProvider(),
                new ConfigCache(), new ServerVersion(), ConfigElementImplementationRegistryMother.withNoPlugins(),
                metricsProbeService, serverHealthService);
        serverHealthService = new ServerHealthService();
        CachedFileGoConfig cachedFileGoConfig = new CachedFileGoConfig(dataSource, serverHealthService);
        cachedGoConfig = cachedFileGoConfig;
        cachedGoConfig.loadConfigIfNull();
        configHelper.usingCruiseConfigDao(new GoConfigDao(cachedFileGoConfig, mock(MetricsProbeService.class)));
    }
}
