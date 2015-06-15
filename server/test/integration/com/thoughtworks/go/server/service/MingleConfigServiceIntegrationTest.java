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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.MingleConfig;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ViewConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class MingleConfigServiceIntegrationTest {
    @Autowired private MingleConfigService mingleConfigService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private Localizer localizer;

    private GoConfigFileHelper configHelper;
    private PipelineConfig pipelineConfig;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper = new GoConfigFileHelper(goConfigDao);
        configHelper.onSetUp();
        configHelper.addPipeline("bar", "stage", MaterialConfigsMother.defaultMaterialConfigs(), "build");

        pipelineConfig = configHelper.addPipeline("foo", "stage", MaterialConfigsMother.defaultMaterialConfigs(), "build");
        configHelper.addMingleConfigToPipeline("foo", new MingleConfig("https://some-tracking-tool:8443", "project-super-secret", "hello=world"));

        CruiseConfig cruiseConfig = configHelper.load();
        PipelineConfigs group = cruiseConfig.findGroup("defaultGroup");
        group.setAuthorization(new Authorization(new ViewConfig(new AdminUser(new CaseInsensitiveString("authorized_user")))));
        configHelper.writeConfigFile(cruiseConfig);

        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("/tmp/foo.passwd"), true);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("admin")));
        configHelper.addSecurity(securityConfig);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldFetchMingleConfigForAGivenPipeline() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MingleConfig mingleConfig = mingleConfigService.mingleConfigForPipelineNamed("foo", new Username(new CaseInsensitiveString("authorized_user")), result);
        assertThat(mingleConfig, is(new MingleConfig("https://some-tracking-tool:8443", "project-super-secret", "hello=world")));
        assertThat(result.isSuccessful(), is(true));

        result = new HttpLocalizedOperationResult();
        mingleConfig = mingleConfigService.mingleConfigForPipelineNamed("foo", new Username(new CaseInsensitiveString("admin")), result);
        assertThat(mingleConfig, is(new MingleConfig("https://some-tracking-tool:8443", "project-super-secret", "hello=world")));
        assertThat(result.isSuccessful(), is(true));
    }
    
    @Test
    public void shouldNotAllowUnauthorizedUserToGetMingleConfig() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MingleConfig mingleConfig = mingleConfigService.mingleConfigForPipelineNamed("foo", new Username(new CaseInsensitiveString("some_loser")), result);
        assertThat(mingleConfig, is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("You do not have view permissions for pipeline 'foo'."));
        assertThat(result.httpCode(), is(401));
    }

    @Test
    public void shouldNotConsiderNoMingleConfigAnError() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MingleConfig mingleConfig = mingleConfigService.mingleConfigForPipelineNamed("bar", new Username(new CaseInsensitiveString("admin")), result);
        assertThat(mingleConfig, is(nullValue()));
        assertThat(result.isSuccessful(), is(true));
    }

}
