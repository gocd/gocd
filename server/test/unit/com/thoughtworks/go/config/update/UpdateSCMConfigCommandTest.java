/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 *
 */

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateSCMConfigCommandTest {

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private SCM scm;
    private SCMs scms;
    private LocalizedOperationResult result;


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
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "md5", entityHashingService);
        assertThat(cruiseConfig.getSCMs().contains(scm), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getSCMs().contains(updatedScm), is(true));
    }

    @Test
    public void shouldThrowAnExceptionIfSCMIsNotFound() throws Exception {
        SCM updatedScm = new SCM("non-existent-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "md5", entityHashingService);
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The pluggable scm material with id 'non-existent-id' is not found.");
        command.update(cruiseConfig);
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        updatedScm.setName("material");
        when(entityHashingService.md5ForEntity(cruiseConfig.getSCMs().find("id"))).thenReturn("another-md5");
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "md5", entityHashingService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
        assertThat(result.toString(), containsString(updatedScm.getName()));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfObjectNotFound() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM updatedScm = new SCM("non-existent-id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result, "md5", entityHashingService);

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The pluggable scm material with id 'non-existent-id' is not found.");
        assertThat(command.canContinue(cruiseConfig), is(false));
    }
}
