/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreatePackageRepositoryCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageRepository packageRepository;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private PackageRepositoryService service;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private GoConfigService goConfigService;
    private String pluginId;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        packageRepository = new PackageRepository();
        repoId = "npmOrg";
        pluginId = "npm";
        packageRepository.setId(repoId);
        packageRepository.setName(repoId);
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, "1"));
        result = new HttpLocalizedOperationResult();
    }

    @Test
    public void shouldCreatePackageRepository() throws Exception {
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1", null, null, null, false));

        assertThat(cruiseConfig.getPackageRepositories().size(), is(0));
        assertNull(cruiseConfig.getPackageRepositories().find(repoId));
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(packageRepository));
    }

    @Test
    public void shouldNotCreatePackageRepositoryIfTheSpecifiedPluginTypeIsInvalid() throws Exception {
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);
        assertThat(cruiseConfig.getPackageRepositories().size(), is(0));
        assertNull(cruiseConfig.getPackageRepositories().find(repoId));
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(LocalizedMessage.string("INVALID_PLUGIN_TYPE", pluginId));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryWithSpecifiedNameAlreadyExists() throws Exception {
        cruiseConfig.getPackageRepositories().add(packageRepository);
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1", null, null, null, false));
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageRepository.errors().firstError(), is("You have defined multiple repositories called 'npmOrg'. Repository names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryHasDuplicateConfigurationProperties() throws Exception {
        Configuration configuration = new Configuration();
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"));
        configuration.add(property);
        configuration.add(property);
        packageRepository.setConfiguration(configuration);
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1", null, null, null, false));
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(property.errors().firstError(), is("Duplicate key 'foo' found for Repository 'npmOrg'"));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryHasInvalidName() throws Exception {
        packageRepository.setName("~!@#$%^&*(");
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1", null, null, null, false));
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageRepository.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepository, currentUser, pluginManager, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }
}

