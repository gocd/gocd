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
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreatePackageConfigCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageDefinition packageDefinition;
    private String packageId;
    private String packageUuid;

    private String repoId;
    private PackageRepository repository;

    private Configuration configuration;
    private HttpLocalizedOperationResult result;

    @Mock
    private PackageDefinitionService packageDefinitionService;

    @Mock
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        result = new HttpLocalizedOperationResult();

        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        packageId = "prettyjson";
        packageUuid = "random-uuid";
        configuration = new Configuration(new ConfigurationProperty(new ConfigurationKey("PACKAGE_ID"), new ConfigurationValue(packageId)));
        packageDefinition = new PackageDefinition(packageUuid, "prettyjson", configuration);
        PackageRepositories repositories = cruiseConfig.getPackageRepositories();

        repoId = "repoId";
        Configuration configuration = new Configuration(new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar")));
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin-id", "1");
        repository = new PackageRepository(repoId, "repoName", pluginConfiguration, configuration);
        repositories.add(repository);
        cruiseConfig.setPackageRepositories(repositories);
    }

    @Test
    public void shouldAddTheSpecifiedPackage() throws Exception {
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);
        assertNull(cruiseConfig.getPackageRepositories().findPackageDefinitionWith(packageId));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getPackageRepositories().find(repoId).getPackages().find(packageUuid), is(packageDefinition));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackages() throws Exception {
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden(EntityType.PackageDefinition.forbiddenToEdit(packageDefinition.getId(), currentUser.getUsername()), forbidden());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateIfPackageNameIsNull() {
        PackageDefinition pkg = new PackageDefinition("Id", null, new Configuration());
        PackageRepository repository = new PackageRepository(repoId, null, null, null);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));

        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, pkg, repoId, currentUser, result, packageDefinitionService);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.errors().getAllOn("name"), is(Arrays.asList("Package name is mandatory")));
    }

    @Test
    public void shouldValidateIfPackageNameIsInvalid() {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find(repoId);
        PackageDefinition pkg = new PackageDefinition("Id", "!$#", new Configuration());
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));

        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, pkg, repoId, currentUser, result, packageDefinitionService);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.errors().getAllOn("name"), is(Arrays.asList("Invalid Package name '!$#'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.")));
    }

    @Test
    public void shouldValidateDuplicatePropertiesInConfiguration() {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find(repoId);
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"));
        Configuration configuration = new Configuration();
        configuration.add(property);
        configuration.add(property);
        PackageDefinition pkg = new PackageDefinition("Id", "name", configuration);
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, pkg, repoId, currentUser, result, packageDefinitionService);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.getAllErrors().toString(), containsString("Duplicate key 'key' found for Package 'name'"));
    }

    @Test
    public void shouldValidateDuplicatePackageName() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find(repoId);
        PackageDefinition pkg = new PackageDefinition("Id", "prettyjson", new Configuration());
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageDefinition.errors().size(), is(1));
        assertThat(packageDefinition.errors().firstError(), is("You have defined multiple packages called 'prettyjson'. Package names are case-insensitive and must be unique within a repository."));
    }

    @Test
    public void shouldValidateDuplicateIdentity() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find(repoId);
        PackageDefinition pkg = new PackageDefinition("Id", "name", configuration);
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(packageDefinition.errors().size(), is(1));
        assertThat(packageDefinition.errors().firstError(), is("Cannot save package or repo, found duplicate packages. [Repo Name: 'repoName', Package Name: 'name'], [Repo Name: 'repoName', Package Name: 'prettyjson']"));
    }

    @Test
    public void shouldNotContinueIfTheRepositoryWithSpecifiedRepoIdDoesNotexist() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        cruiseConfig.setPackageRepositories(new PackageRepositories());
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(EntityType.PackageRepository.notFoundMessage(repoId));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repoId, currentUser, result, packageDefinitionService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}
