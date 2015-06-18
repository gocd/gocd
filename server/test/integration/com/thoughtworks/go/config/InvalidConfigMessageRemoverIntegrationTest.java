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

import java.io.File;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class InvalidConfigMessageRemoverIntegrationTest {

    @Autowired
    GoConfigService goConfigService;
    @Autowired ServerHealthService serverHealthService;
    @Autowired
    CachedGoConfig cachedGoConfig;

    private File configFile;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configFile = TestFileUtil.createTempFileInSubfolder("cruise-config.xml");
        GoConfigFileHelper.clearConfigVersions();
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        new ConfigRepository(systemEnvironment).initialize();
        serverHealthService.removeAllLogs();
    }

    @After
    public void tearDown() throws Exception {
        configFile.delete();
        configHelper.onTearDown();
        serverHealthService.removeAllLogs();
        GoConfigFileHelper.clearConfigVersions();
    }

    @Test
    public void shouldRemoveServerHealthServiceMessageAboutStartedWithInvalidConfiguration() {
        serverHealthService.update(ServerHealthState.warning("Invalid Configuration", "something",HealthStateType.general(HealthStateScope.forInvalidConfig())));
        InvalidConfigMessageRemover remover = new InvalidConfigMessageRemover(goConfigService, serverHealthService);
        remover.initialize();
        assertThat(serverHealthService.getAllLogs().isEmpty(), is(false));
        configHelper.addAgent("hostname", "uuid"); //Any change to the config file
        cachedGoConfig.forceReload();
        assertThat(serverHealthService.filterByScope(HealthStateScope.forInvalidConfig()).isEmpty(), is(true));
    }
}
