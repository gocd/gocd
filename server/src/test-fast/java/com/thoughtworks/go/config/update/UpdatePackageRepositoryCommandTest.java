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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.packageMaterialConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdatePackageRepositoryCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageRepository newPackageRepo;
    private PackageRepository oldPackageRepo;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private EntityHashingService entityHashingService;

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
        newPackageRepo = new PackageRepository(repoId, repoId, new PluginConfiguration("npm", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("REPO_URL"), new ConfigurationValue("http://bar"))));
        oldPackageRepo = new PackageRepository(repoId, repoId, new PluginConfiguration("npm", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("REPO_URL"), new ConfigurationValue("http://foo"))));
        result = new HttpLocalizedOperationResult();
        cruiseConfig.setPackageRepositories(new PackageRepositories(oldPackageRepo));
    }

    @Test
    public void shouldUpdatePackageRepository() throws Exception {
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(oldPackageRepo));

        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(newPackageRepo));
    }

    @Test
    public void shouldCopyPackagesFromOldRepositoryToTheUpdatedRepository() throws Exception {
        PackageDefinition nodePackage = new PackageDefinition("foo", "bar", new Configuration());
        oldPackageRepo.setPackages(new Packages(nodePackage));
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(oldPackageRepo));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId).getPackages().size(), is(1));
        assertThat(newPackageRepo.getPackages().size(), is(0));

        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(newPackageRepo));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId).getPackages().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId).getPackages().first(), is(nodePackage));
    }

    @Test
    public void shouldUpdatePackageRepositoryConfigurationOnAssociatedPipelines() {
        GitMaterialConfig git = git("http://example.com");
        PackageMaterialConfig packageMaterial = packageMaterialConfig();
        PipelineConfig p1 = pipelineConfig("p1", new MaterialConfigs(git, packageMaterial));
        PipelineConfig p2 = pipelineConfig("p2", new MaterialConfigs(git));
        PipelineConfig p3 = pipelineConfig("p3", new MaterialConfigs(packageMaterial));

        PipelineConfigs group1 = createGroup("group1", p1, p2);
        PipelineConfigs group2 = createGroup("group2", p3);

        cruiseConfig.setGroup(new PipelineGroups(group1, group2));
        cruiseConfig.setPackageRepositories(new PackageRepositories(packageMaterial.getPackageDefinition().getRepository()));

        Configuration updatedConfiguration = new Configuration(create("new_key1", "new_value1"), create("new_key2", "new_value2"));
        PackageRepository updatePackageRepo = PackageRepositoryMother.create(packageMaterial.getPackageDefinition().getRepository().getId(),
                "repo", "id", "version", updatedConfiguration);

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService,
                updatePackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        command.update(cruiseConfig);

        PackageMaterialConfig materialConfig1 = cruiseConfig
                .getPipelineConfigByName(new CaseInsensitiveString("p1")).packageMaterialConfigs().get(0);

        assertThat(materialConfig1.getPackageDefinition().getRepository(), is(updatePackageRepo));

        PackageMaterialConfig materialConfig2 = cruiseConfig
                .getPipelineConfigByName(new CaseInsensitiveString("p3")).packageMaterialConfigs().get(0);

        assertThat(materialConfig2.getPackageDefinition().getRepository(), is(updatePackageRepo));
    }
    @Test
    public void shouldNotUpdatePackageRepositoryIfTheSpecifiedPluginTypeIsInvalid() throws Exception {
        when(packageRepositoryService.validatePluginId(newPackageRepo)).thenReturn(false);
        when(packageRepositoryService.validateRepositoryConfiguration(newPackageRepo)).thenReturn(true);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryWithSpecifiedNameAlreadyExists() throws Exception {
        cruiseConfig.getPackageRepositories().add(newPackageRepo);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("You have defined multiple repositories called 'npmOrg'. Repository names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasDuplicateConfigurationProperties() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"));
        Configuration configuration = new Configuration(property, property);
        newPackageRepo.setConfiguration(configuration);

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(property.errors().firstError(), is("Duplicate key 'foo' found for Repository 'npmOrg'"));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasInvalidName() throws Exception {
        newPackageRepo.setName("~!@#$%^&*(");
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden(EntityType.PackageRepository.forbiddenToEdit(newPackageRepo.getId(), currentUser.getUsername()), forbidden());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfTheUserSubmittedStaleEtag() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.getPackageRepository(repoId)).thenReturn(oldPackageRepo);
        when(entityHashingService.hashForEntity(oldPackageRepo)).thenReturn("foobar");
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.stale(EntityType.PackageRepository.staleConfig(repoId));

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldNotContinueIfRepoIdIsChanged() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.getPackageRepository(repoId)).thenReturn(oldPackageRepo);
        when(entityHashingService.hashForEntity(oldPackageRepo)).thenReturn("digest");
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.unprocessableEntity("Changing the repository id is not supported by this API.");

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, "old-repo-id");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);
        when(entityHashingService.hashForEntity(nullable(PackageRepository.class))).thenReturn("digest");

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);
        when(entityHashingService.hashForEntity(nullable(PackageRepository.class))).thenReturn("digest");

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "digest", entityHashingService, result, repoId);

        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}
