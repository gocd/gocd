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
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateSCMConfigCommandTest {

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private SCM scm;
    private SCMs scms;

    @Mock
    private PluggableScmService pluggableScmService;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private LocalizedOperationResult result;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        scm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        scms = new SCMs();
        scms.add(scm);
        cruiseConfig.setSCMs(scms);
    }

    @Test
    public void shouldUpdateAnExistingSCMWithNewValues() throws Exception {
        SCM updatedScm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result);
        assertThat(cruiseConfig.getSCMs().contains(scm), is(true));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getSCMs().contains(updatedScm), is(true));
    }

    @Test
    public void shouldThrowAnExceptionIfSCMIsNotFound() throws Exception {
        SCM updatedScm = new SCM("non-existent-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"),new ConfigurationValue("value1"))));
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(updatedScm, pluggableScmService, goConfigService, currentUser, result);
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The pluggable scm material with id 'non-existent-id' is not found.");
        command.update(cruiseConfig);
    }
}