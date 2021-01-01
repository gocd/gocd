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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.pluggableSCMMaterialConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateSCMConfigCommandTest {

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private SCM scm;
    private SCMs scms;
    private HttpLocalizedOperationResult result;

    @Mock
    private PluggableScmService pluggableScmService;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private EntityHashingService entityHashingService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        initMocks(this);
        result = new HttpLocalizedOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        scm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        scm.setName("material");
        scms = new SCMs();
        scms.add(scm);
        cruiseConfig.setSCMs(scms);
    }

    @Test
    public void shouldUpdateAnExistingSCMWithNewValues() throws Exception {
        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        updatedScm.setName("material");
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);
        assertThat(cruiseConfig.getSCMs().contains(scm), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getSCMs().contains(updatedScm), is(true));
    }

    @Test
    public void shouldUpdateSCMConfigurationOnAssociatedPipelines() {
        SCM updatedSCM = SCMMother.create("id", "prop3", "prop4");
        cruiseConfig.addPipelineWithoutValidation("group1", PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(
                pluggableSCMMaterialConfig("id", "prop1", "prop2"))
        ));
        cruiseConfig.addPipelineWithoutValidation("group2", PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(
                pluggableSCMMaterialConfig("id", "prop1", "prop2"))
        ));

        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedSCM, pluggableScmService, goConfigService,
                currentUser, result, "digest", entityHashingService);

        command.update(cruiseConfig);

        PluggableSCMMaterialConfig materialConfig1 = (PluggableSCMMaterialConfig) cruiseConfig
                .getPipelineConfigByName(new CaseInsensitiveString("p1")).materialConfigs().get(0);

        assertThat(materialConfig1.getSCMConfig(), is(updatedSCM));

        PluggableSCMMaterialConfig materialConfig2 = (PluggableSCMMaterialConfig) cruiseConfig
                .getPipelineConfigByName(new CaseInsensitiveString("p2")).materialConfigs().get(0);
        assertThat(materialConfig2.getSCMConfig(), is(updatedSCM));
    }

    @Test
    public void shouldThrowAnExceptionIfSCMIsNotFound() throws Exception {
        SCM updatedScm = new SCM("non-existent-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The pluggable scm material with id 'non-existent-id' is not found.");
        command.update(cruiseConfig);
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.SCM.forbiddenToEdit(scm.getId(), currentUser.getUsername())));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);
        when(entityHashingService.hashForEntity(any(SCM.class))).thenReturn("digest");

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);
        when(entityHashingService.hashForEntity(any(SCM.class))).thenReturn("digest");

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        updatedScm.setName("material");
        when(entityHashingService.hashForEntity(cruiseConfig.getSCMs().find("id"))).thenReturn("another-digest");
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("Someone has modified the configuration for"));;
        assertThat(result.toString(), containsString(updatedScm.getName()));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfObjectNotFound() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("non-existent-id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "digest", entityHashingService);

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The pluggable scm material with id 'non-existent-id' is not found.");
        assertThat(command.canContinue(cruiseConfig), is(false));
    }
}
