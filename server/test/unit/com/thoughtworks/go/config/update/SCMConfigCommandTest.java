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
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SCMConfigCommandTest {

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

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        scm = new SCM("id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        scms = new SCMs();
        scms.add(scm);
    }

    @Test
    public void shouldValidateGivenSCMConfigurationAgainstSpecifiedPlugin() {
        scm.setName("material");
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);
        assertThat(command.isValid(cruiseConfig), is(false));
    }

    @Test
    public void shouldValidateIfSCMNameIsNull() {
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(scm.errors().getAllOn("name"), is(Arrays.asList("Please provide name")));
    }

    @Test
    public void shouldClearErrorsFromCruiseConfigAfterValidation() {
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);

        assertThat(command.isValid(cruiseConfig), is(false));

        command.clearErrors();
        assertTrue(cruiseConfig.errors().isEmpty());

    }

    @Test
    public void shouldValidateSCMName() {
        scm.setName("+!@");
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(scm.errors().getAllOn("name"), is(Arrays.asList("Invalid SCM name '+!@'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.")));

    }

    @Test
    public void shouldValidateDuplicateMaterialConfigurations() {
        scm.setName("material");
        SCM duplicateSCM = new SCM("another-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        duplicateSCM.setName("duplicate.material");
        scms.add(duplicateSCM);
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(duplicateSCM, pluggableScmService, result, currentUser, goConfigService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(duplicateSCM.errors().getAllOn("scmId"), is(Arrays.asList("Cannot save SCM, found duplicate SCMs. material, duplicate.material")));
    }

    @Test
    public void shouldValidateAgainstDuplicateSCMnames() {
        scm.setName("material");
        SCM duplicateSCM = new SCM("another-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        duplicateSCM.setName("material");
        scms.add(duplicateSCM);
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(duplicateSCM, pluggableScmService, result, currentUser, goConfigService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(duplicateSCM.errors().getAllOn("name"), is(Arrays.asList("Cannot save SCM, found multiple SCMs called 'material'. SCM names are case-insensitive and must be unique.")));
    }

    @Test
    public void shouldValidateAgainstDuplicateSCMIds() {
        scm.setName("material");
        SCM duplicateSCM = new SCM("id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"),new ConfigurationValue("value"))));
        duplicateSCM.setName("another.material");
        scms.add(duplicateSCM);
        cruiseConfig.setSCMs(scms);

        CreateSCMConfigCommand command = new CreateSCMConfigCommand(duplicateSCM, pluggableScmService, result, currentUser, goConfigService);
        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(duplicateSCM.errors().getAllOn("scmId"), is(Arrays.asList("Cannot save SCM, found duplicate SCMs. material, another.material")));
    }

}