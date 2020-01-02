/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdatePackageConfigCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageDefinition oldPackageDefinition;
    private PackageDefinition newPackageDefinition;
    private String packageUuid;
    private String newPackageName;
    private Configuration configuration;
    private HttpLocalizedOperationResult result;

    @Mock
    private PackageDefinitionService packageDefinitionService;

    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();

        packageUuid = "random-uuid";
        configuration = new Configuration(new ConfigurationProperty(new ConfigurationKey("PACKAGE_ID"), new ConfigurationValue("foo")));
        oldPackageDefinition = new PackageDefinition(packageUuid, "prettyjson", configuration);

        newPackageName = "prettyjson";
        newPackageDefinition = new PackageDefinition(packageUuid, newPackageName, configuration);

        result = new HttpLocalizedOperationResult();
        PackageRepositories repositories = cruiseConfig.getPackageRepositories();

        PackageRepository repository = new PackageRepository("repoId", "repoName", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"))));
        repository.addPackage(oldPackageDefinition);
        oldPackageDefinition.setRepository(repository);
        repositories.add(repository);
        cruiseConfig.setPackageRepositories(repositories);
    }

    @Test
    public void shouldUpdateTheSpecifiedPackage() throws Exception {
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        assertThat(cruiseConfig.getPackageRepositories().findPackageDefinitionWith(packageUuid), is(oldPackageDefinition));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getPackageRepositories().findPackageDefinitionWith(packageUuid), is(newPackageDefinition));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackages() throws Exception {
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(entityHashingService.md5ForEntity(oldPackageDefinition)).thenReturn("md5");
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden(EntityType.PackageDefinition.forbiddenToEdit(newPackageDefinition.getId(), currentUser.getUsername()), forbidden());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfTheUserSubmitsStaleEtag() throws Exception {
        newPackageDefinition.setRepository(new PackageRepository(oldPackageDefinition.getRepository().getId(), "name", null, null));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "stale-etag", this.entityHashingService, result, packageDefinitionService);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(entityHashingService.md5ForEntity(oldPackageDefinition)).thenReturn("md5");
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.stale(EntityType.PackageDefinition.staleConfig(oldPackageDefinition.getId()));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateIfPackageNameIsNull() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find("repoId");
        PackageDefinition pkg = new PackageDefinition("Id", null, new Configuration());
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, pkg, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.errors().size(), is(1));
        assertThat(pkg.errors().firstError(), is("Package name is mandatory"));
    }

    @Test
    public void shouldValidateIfPackageNameIsInvalid() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find("repoId");
        PackageDefinition pkg = new PackageDefinition("Id", "!$#", new Configuration());
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, pkg, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.errors().size(), is(1));
        assertThat(pkg.errors().firstError(), is("Invalid Package name '!$#'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateDuplicatePropertiesInConfiguration() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find("repoId");
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"));
        Configuration configuration = new Configuration();
        configuration.add(property);
        configuration.add(property);
        PackageDefinition pkg = new PackageDefinition("Id", newPackageName, configuration);
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, pkg, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(pkg.getAllErrors().toString(), containsString("Duplicate key 'key' found for Package 'prettyjson'"));
    }

    @Test
    public void shouldValidateDuplicatePackageName() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find("repoId");
        PackageDefinition pkg = new PackageDefinition("Id", newPackageName, new Configuration());
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageDefinition.errors().size(), is(1));
        assertThat(newPackageDefinition.errors().firstError(), is("You have defined multiple packages called 'prettyjson'. Package names are case-insensitive and must be unique within a repository."));
    }

    @Test
    public void shouldValidateDuplicateIdentity() throws Exception {
        PackageRepository repository = cruiseConfig.getPackageRepositories().find("repoId");
        PackageDefinition pkg = new PackageDefinition("Id", "name", configuration);
        pkg.setRepository(repository);
        repository.addPackage(pkg);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageDefinition.errors().size(), is(1));
        assertThat(newPackageDefinition.errors().firstError(), is("Cannot save package or repo, found duplicate packages. [Repo Name: 'repoName', Package Name: 'name'], [Repo Name: 'repoName', Package Name: 'prettyjson']"));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(this.entityHashingService.md5ForEntity(any(PackageDefinition.class))).thenReturn("md5");

        newPackageDefinition.setRepository(new PackageRepository(oldPackageDefinition.getRepository().getId(), "name", null, null));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        when(this.entityHashingService.md5ForEntity(any(PackageDefinition.class))).thenReturn("md5");

        newPackageDefinition.setRepository(new PackageRepository(oldPackageDefinition.getRepository().getId(), "name", null, null));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, packageUuid, newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldNotContinueIfPackageIdIsChanged() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(oldPackageDefinition)).thenReturn("md5");
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.unprocessableEntity("Changing the package id is not supported by this API.");

        newPackageDefinition.setRepository(new PackageRepository(oldPackageDefinition.getRepository().getId(), "name", null, null));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, "old-package-id", newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldNotContinueIfTheRepositoryToWhichThePackageBelongsDoesNotExist() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        cruiseConfig.setPackageRepositories(new PackageRepositories());
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        newPackageDefinition.setRepository(new PackageRepository("id", "name", null, null));
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, "old-package-id", newPackageDefinition, currentUser, "md5", this.entityHashingService, result, packageDefinitionService);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(EntityType.PackageRepository.notFoundMessage("id"));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
        newPackageDefinition.setRepository(null);
    }
}
