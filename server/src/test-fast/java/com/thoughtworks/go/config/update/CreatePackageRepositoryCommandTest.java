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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setup() throws Exception {
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
        assertThat(packageRepository.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden(EntityType.PackageRepository.forbiddenToEdit(packageRepository.getId(), currentUser.getUsername()), forbidden());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        lenient().when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, packageRepositoryService, packageRepository, currentUser, result);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}

