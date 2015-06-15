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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.Localizer;
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

import static com.thoughtworks.go.server.service.EnvironmentConfigServiceTest.env;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class EnvironmentConfigServiceIntegrationTest {

    @Autowired private SecurityService securityService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private EnvironmentConfigService service;
    @Autowired private Localizer localizer;
    @Autowired private AgentService agentService;
    
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();


    @Before
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForNoPermission() throws IOException {
        configHelper.turnOnSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("evil_hacker")), result);
        assertThat(result.message(localizer), is("Failed to add environment. User 'evil_hacker' does not have permission to add environments"));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForDuplicateEnvironment() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.message(localizer), is("Failed to add environment. Environment 'foo-env' already exists."));
    }

    @Test
    public void shouldReturnBadRequestForInvalidEnvName() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.message(localizer), containsString("Failed to add environment."));
    }
    
    @Test
    public void shouldUpdateExistingEnvironment() throws Exception{
        BasicEnvironmentConfig uat = environmentConfig("uat");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("foo", "dev", "job"), "foo-grp");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("bar", "dev", "job"), "foo-grp");
        goConfigService.addAgent(new AgentConfig("uuid-1", "host-1", "192.168.1.2"));
        goConfigService.addAgent(new AgentConfig("uuid-2", "host-2", "192.168.1.3"));
        uat.addPipeline(new CaseInsensitiveString("foo"));
        uat.addAgent("uuid-2");
        uat.addEnvironmentVariable("env-one", "ONE");
        uat.addEnvironmentVariable("env-two", "TWO");
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("dev")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("qa")));
        goConfigService.addEnvironment(uat);
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("acceptance")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("function_testing")));
        EnvironmentConfig newUat = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
        newUat.addPipeline(new CaseInsensitiveString("bar"));
        newUat.addAgent("uuid-1");
        newUat.addEnvironmentVariable("env-three", "THREE");
        HttpLocalizedOperationResult result = service.updateEnvironment("uat", newUat, new Username(new CaseInsensitiveString("foo")), goConfigDao.md5OfConfigFile());
        EnvironmentConfig updatedEnv = service.named("prod");
        assertThat(updatedEnv.name(), is(new CaseInsensitiveString("prod")));
        assertThat(updatedEnv.getAgents().getUuids(), is(Arrays.asList("uuid-1")));
        assertThat(updatedEnv.getPipelineNames(), is(Arrays.asList(new CaseInsensitiveString("bar"))));
        EnvironmentVariablesConfig updatedVariables = new EnvironmentVariablesConfig();
        updatedVariables.add("env-three", "THREE");
        assertThat(updatedEnv.getVariables(), is(updatedVariables));
        EnvironmentsConfig currentEnvironments = goConfigService.getCurrentConfig().getEnvironments();
        assertThat(currentEnvironments.indexOf(updatedEnv), is(2));
        assertThat(currentEnvironments.size(), is(5));
    }
    
    @Test
    public void shouldReturnTheCorrectLocalizedMessageWhenUserDoesNotHavePermissionToUpdate() throws IOException {
        configHelper.addEnvironments("foo");
        configHelper.turnOnSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = service.updateEnvironment("foo", env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("evil_hacker")),  goConfigDao.md5OfConfigFile());
        assertThat(result.message(localizer), is("Failed to update environment 'foo'. User 'evil_hacker' does not have permission to update environments"));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForUpdateWhenDuplicateEnvironmentExists() {
        configHelper.addEnvironments("foo-env");
        configHelper.addEnvironments("bar-env");
        HttpLocalizedOperationResult result = service.updateEnvironment("bar-env", env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")),  goConfigDao.md5OfConfigFile());
        assertThat(result.message(localizer), is("Failed to update environment 'bar-env'. Duplicate unique value [foo-env] declared for identity constraint \"uniqueEnvironmentName\" of element \"environments\"."));
    }

    @Test
    public void shouldReturnBadRequestForUpdateWhenUsingInvalidEnvName() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = service.updateEnvironment("foo-env", env("foo env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")),  goConfigDao.md5OfConfigFile());
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.message(localizer), containsString("Failed to update environment 'foo-env'."));
    }
    
    @Test
    public void shouldReturnAClonedInstanceOfEnvironmentConfig() throws NoSuchEnvironmentException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.addEnvironments("foo-env");
        assertThat(service.named("foo-env"), sameInstance(service.named("foo-env")));
        assertThat(service.named("foo-env"), not(sameInstance(service.forEdit("foo-env", result).getConfigElement())));
        assertThat(result.isSuccessful(), is(true));
    }
    
    @Test
    public void shouldPopulateResultWithErrorIfEnvNotFound() throws NoSuchEnvironmentException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> edit = service.forEdit("foo-env", result);
        assertThat(result.message(localizer), is("Environment named 'foo-env' not found."));
        assertThat(edit, is(nullValue()));
    }
    
    @Test
    public void shouldNotUpdateEnvironmentIfEditingOverAStaleCopy() throws Exception{
        configHelper.addEnvironments("prod");
        String staleMd5 = goConfigDao.md5OfConfigFile();
        service.updateEnvironment("prod", environmentConfig("uat1"), new Username(new CaseInsensitiveString("foo")), staleMd5);
        assertThat("md5 should be stale", goConfigDao.md5OfConfigFile(), is(not(staleMd5)));

        HttpLocalizedOperationResult result = service.updateEnvironment("prod", environmentConfig("uat2"), new Username(new CaseInsensitiveString("foo")),  staleMd5);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Failed to update environment 'prod'. " + ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH));
    }

    private BasicEnvironmentConfig environmentConfig(String name) {
        return new BasicEnvironmentConfig(new CaseInsensitiveString(name));
    }
}
