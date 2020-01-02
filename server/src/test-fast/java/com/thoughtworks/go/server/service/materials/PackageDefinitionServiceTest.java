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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;

import static com.thoughtworks.go.server.service.materials.PackageMaterialTestHelper.assertPackageConfiguration;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PackageDefinitionServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    private PackageDefinitionService service;
    private PackageRepository packageRepository;

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new PackageDefinitionService(packageRepositoryExtension, entityHashingService, goConfigService);

        PackageConfigurations configurations = new PackageConfigurations();
        configurations.add(new PackageConfiguration("required").with(PackageConfiguration.REQUIRED, true));
        configurations.add(new PackageConfiguration("required_secure").with(PackageConfiguration.REQUIRED, true).with(PackageConfiguration.SECURE, true));
        configurations.add(new PackageConfiguration("secure").with(PackageConfiguration.SECURE, true).with(PackageConfiguration.REQUIRED, false));
        configurations.add(new PackageConfiguration("not_required_not_secure").with(PackageConfiguration.REQUIRED, false));
        configurations.add(new PackageConfiguration("spec"));
        String pluginId = "yum";
        PackageMetadataStore.getInstance().addMetadataFor(pluginId, configurations);
        packageRepository = PackageRepositoryMother.create("1", "repo", pluginId, "1", new Configuration());
    }

    @Test
    public void shouldPerformPluginValidationsUsingMetaDataBeforeSavingPackageRepository() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("required", false, ""));
        configuration.add(ConfigurationPropertyMother.create("required_secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("not_required_not_secure", false, ""));
        configuration.add(ConfigurationPropertyMother.create("spec", false, "xyz?"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "name", configuration, packageRepository);

        ValidationResult expectedValidationResult = new ValidationResult();
        expectedValidationResult.addError(new ValidationError("spec", "invalid spec"));
        when(packageRepositoryExtension.isPackageConfigurationValid(eq(packageRepository.getPluginConfiguration().getId()),
                any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class),
                any(RepositoryConfiguration.class))).thenReturn(expectedValidationResult);
        service.performPluginValidationsFor(packageDefinition);

        assertThat(packageDefinition.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("Field: 'required' is required")));
        assertThat(packageDefinition.getConfiguration().get(1).getEncryptedConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("Field: 'required_secure' is required")));
        assertThat(packageDefinition.getConfiguration().get(2).getEncryptedConfigurationValue().errors().isEmpty(), is(true));
        assertThat(packageDefinition.getConfiguration().get(3).getConfigurationValue().errors().isEmpty(), is(true));
        assertThat(packageDefinition.getConfiguration().get(4).getConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("invalid spec")));
    }

    @Test
    public void shouldPerformPluginValidationsBeforeValidationsByGoAndGoDoesNotAddErrorIfAlreadyPresent() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("required-field", false, ""));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "name", configuration, packageRepository);

        ValidationResult expectedValidationResult = new ValidationResult();
        expectedValidationResult.addError(new ValidationError("required-field", "error-one"));
        expectedValidationResult.addError(new ValidationError("required-field", "error-two"));

        when(packageRepositoryExtension.isPackageConfigurationValid(eq(packageRepository.getPluginConfiguration().getId()),
                any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class),
                any(RepositoryConfiguration.class))).thenReturn(expectedValidationResult);
        service.performPluginValidationsFor(packageDefinition);
        assertThat(packageDefinition.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value").size(), is(2));
        assertThat(packageDefinition.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value"), is(hasItems("error-one", "error-two")));
    }

    @Test
    public void shouldPerformCheckConnectionOnPackage() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("required", false, ""));
        configuration.add(ConfigurationPropertyMother.create("required_secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("not_required_not_secure", false, ""));
        configuration.add(ConfigurationPropertyMother.create("spec", false, "xyz?"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "name", configuration, packageRepository);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();


        PackageDefinitionService service = new PackageDefinitionService(packageRepositoryExtension, entityHashingService, goConfigService);

        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenReturn(
                new Result().withSuccessMessages("Got Package!!!"));

        service.checkConnection(packageDefinition, result);

        assertPackageConfiguration(packageConfigurationsCaptor.getValue().list(), packageDefinition.getConfiguration());
        assertPackageConfiguration(packageRepositoryConfigurationsCaptor.getValue().list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("OK. Got Package!!!"));
        verify(packageRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
    }

    @Test
    public void shouldPerformCheckConnectionForPackageAndReportFailure() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("required", false, ""));
        configuration.add(ConfigurationPropertyMother.create("required_secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("not_required_not_secure", false, ""));
        configuration.add(ConfigurationPropertyMother.create("spec", false, "xyz?"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "name", configuration, packageRepository);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();


        PackageDefinitionService service = new PackageDefinitionService(packageRepositoryExtension, entityHashingService, goConfigService);
        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenReturn(
                new Result().withErrorMessages("Package not available", "Repo not available"));

        service.checkConnection(packageDefinition, result);

        assertPackageConfiguration(packageConfigurationsCaptor.getValue().list(), packageDefinition.getConfiguration());
        assertPackageConfiguration(packageRepositoryConfigurationsCaptor.getValue().list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Package check Failed. Reason(s): Package not available\nRepo not available"));
        verify(packageRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
    }

    @Test
    public void shouldPerformCheckConnectionForPackageAndCatchException() throws Exception {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("required", false, ""));
        configuration.add(ConfigurationPropertyMother.create("required_secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("secure", true, ""));
        configuration.add(ConfigurationPropertyMother.create("not_required_not_secure", false, ""));
        configuration.add(ConfigurationPropertyMother.create("spec", false, "xyz?"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "name", configuration, packageRepository);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();


        PackageDefinitionService service = new PackageDefinitionService(packageRepositoryExtension, entityHashingService, goConfigService);


        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenThrow(
                new RuntimeException("Check connection for package not implemented!!"));

        service.checkConnection(packageDefinition, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Package check Failed. Reason(s): Check connection for package not implemented!!"));
        verify(packageRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
    }

}

