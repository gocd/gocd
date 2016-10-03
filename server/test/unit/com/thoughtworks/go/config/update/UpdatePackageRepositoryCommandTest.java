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
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdatePackageRepositoryCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageRepository newPackageRepo;
    private PackageRepository oldPackageRepo;
    private String newRepoId;
    private String oldRepoId;
    private HttpLocalizedOperationResult result;
    private String md5;

    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private PackageRepositoryService packageRepositoryService;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private GoConfigService goConfigService;
    private String pluginId;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        md5 = "md5";
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        newPackageRepo = new PackageRepository();
        oldPackageRepo = new PackageRepository();
        newRepoId = oldRepoId = "npmOrg";
        pluginId = "npm";
        newPackageRepo.setId(newRepoId);
        newPackageRepo.setName(newRepoId);
        oldPackageRepo.setId(oldRepoId);
        oldPackageRepo.setName(oldRepoId);
        newPackageRepo.setPluginConfiguration(new PluginConfiguration(pluginId, "1"));
        oldPackageRepo.setPluginConfiguration(new PluginConfiguration(pluginId, "1"));
        result = new HttpLocalizedOperationResult();
        cruiseConfig.setPackageRepositories(new PackageRepositories(oldPackageRepo));
    }

    @Test
    public void shouldUpdatePackageRepository() throws Exception {
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(oldRepoId), is(oldPackageRepo));

        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(newRepoId), is(newPackageRepo));
    }

    @Test
    public void shouldCopyPackagesFromOldRepositoryToTheUpdatedRepository() throws Exception {
        PackageDefinition nodePackage = new PackageDefinition();
        nodePackage.setId("foo");
        nodePackage.setName("bar");
        oldPackageRepo.setPackages(new Packages(nodePackage));
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(oldRepoId), is(oldPackageRepo));
        assertThat(cruiseConfig.getPackageRepositories().find(oldRepoId).getPackages().size(), is(1));
        assertThat(newPackageRepo.getPackages().size(), is(0));

        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        assertThat(result, is(expectedResult));
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(newRepoId), is(newPackageRepo));
        assertThat(cruiseConfig.getPackageRepositories().find(newRepoId).getPackages().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(newRepoId).getPackages().first(), is(nodePackage));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryIfTheSpecifiedPluginTypeIsInvalid() throws Exception {
        when(packageRepositoryService.validatePluginId(newPackageRepo)).thenReturn(false);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);
        assertFalse(command.isValid(cruiseConfig));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryWithSpecifiedNameAlreadyExists() throws Exception {
        cruiseConfig.getPackageRepositories().add(newPackageRepo);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("You have defined multiple repositories called 'npmOrg'. Repository names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasDuplicateConfigurationProperties() throws Exception {
        Configuration configuration = new Configuration();
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar"));
        configuration.add(property);
        configuration.add(property);
        newPackageRepo.setConfiguration(configuration);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(property.errors().firstError(), is("Duplicate key 'foo' found for Repository 'npmOrg'"));
    }

    @Test
    public void shouldNotUpdatePackageRepositoryWhenRepositoryHasInvalidName() throws Exception {
        newPackageRepo.setName("~!@#$%^&*(");
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
        assertThat(newPackageRepo.errors().firstError(), is("Invalid PackageRepository name '~!@#$%^&*('. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackageRepositories() throws Exception {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfTheUserSubmitttedStaleEtag() throws Exception {
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, packageRepositoryService, newPackageRepo, currentUser, md5, entityHashingService, result);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        when(goConfigService.getPackageRepository(oldRepoId)).thenReturn(oldPackageRepo);
        when(entityHashingService.md5ForEntity(oldPackageRepo)).thenReturn("foobar");
        assertThat(command.canContinue(cruiseConfig), is(false));
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Package Repository", oldRepoId));

        assertThat(result, is(expectResult));
    }
}
