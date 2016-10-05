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
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
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
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);

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
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);

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
    public void shouldNotUpdatePackageRepositoryIfTheSpecifiedPluginTypeIsInvalid() throws Exception {
        when(packageRepositoryService.validatePluginId(newPackageRepo)).thenReturn(false);
        when(packageRepositoryService.validateRepositoryConfiguration(newPackageRepo)).thenReturn(true);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryWithSpecifiedNameAlreadyExists() throws Exception {
        cruiseConfig.getPackageRepositories().add(newPackageRepo);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("You have defined multiple repositories called 'npmOrg'. Repository names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasDuplicateConfigurationProperties() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"));
        Configuration configuration = new Configuration(property, property);
        newPackageRepo.setConfiguration(configuration);

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(property.errors().firstError(), is("Duplicate key 'foo' found for Repository 'npmOrg'"));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasInvalidName() throws Exception {
        newPackageRepo.setName("~!@#$%^&*(");
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfTheUserSubmittedStaleEtag() throws Exception {
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        when(goConfigService.getPackageRepository(repoId)).thenReturn(oldPackageRepo);
        when(entityHashingService.md5ForEntity(oldPackageRepo)).thenReturn("foobar");
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Package Repository", repoId));

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, repoId);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldNotContinueIfRepoIdIsChanged() {
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        when(goConfigService.getPackageRepository(repoId)).thenReturn(oldPackageRepo);
        when(entityHashingService.md5ForEntity(oldPackageRepo)).thenReturn("md5");
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.unprocessableEntity(LocalizedMessage.string("Changing the repository id is not supported by this API."));

        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, "md5", entityHashingService, result, "old-repo-id");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }
}
