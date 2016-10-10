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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
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

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreatePackageRepositoryCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageRepository packageRepository;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private PackageRepositoryService packageRepositoryService;

    @Mock
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        repoId = "npmOrg";
        packageRepository = new PackageRepository(repoId, repoId, new PluginConfiguration("npm", "1"), new Configuration());
        result = new HttpLocalizedOperationResult();
        cruiseConfig.getPackageRepositories().add(packageRepository);
    }

    @Test
    public void shouldCreatePackageRepository() throws Exception {
        PackageRepository repository = new PackageRepository("id", "name", new PluginConfiguration(), new Configuration());
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, repository, currentUser, result);

        assertNull(cruiseConfig.getPackageRepositories().find("id"));
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().find("id"), is(repository));
    }

    @Test
    public void shouldNotCreatePackageRepositoryIfTheSpecifiedPluginTypeIsInvalid() throws Exception {
        when(packageRepositoryService.validatePluginId(packageRepository)).thenReturn(false);
        when(packageRepositoryService.validateRepositoryConfiguration(packageRepository)).thenReturn(true);
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);
        assertFalse(command.isValid(cruiseConfig));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryWithSpecifiedNameAlreadyExists() throws Exception {
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageRepository.errors().firstError(), is("You have defined multiple repositories called 'npmOrg'. Repository names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryHasDuplicateConfigurationProperties() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"));
        Configuration configuration = new Configuration(property, property);
        packageRepository.setConfiguration(configuration);
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(property.errors().firstError(), is("Duplicate key 'foo' found for Repository 'npmOrg'"));
    }

    @Test
    public void shouldNotCreatePackageRepositoryWhenRepositoryHasInvalidName() throws Exception {
        packageRepository.setName("~!@#$%^&*(");
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageRepository.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }
}

