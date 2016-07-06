/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;

import static com.thoughtworks.go.server.service.materials.PackageMaterialTestHelper.assertPackageConfiguration;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PackageDefinitionServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private Localizer localizer;
    @Mock
    private PackageMaterialConfiguration packageMaterialConfiguration;
    @Mock
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private PackageDefinitionService service;
    private PackageRepository packageRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(localizer.localize(eq("MANDATORY_CONFIGURATION_FIELD_WITH_NAME"), anyString())).thenReturn("mandatory field");
        service = new PackageDefinitionService(packageAsRepositoryExtension, localizer);

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
        when(packageAsRepositoryExtension.isPackageConfigurationValid(eq(packageRepository.getPluginConfiguration().getId()),
                any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class),
                any(RepositoryConfiguration.class))).thenReturn(expectedValidationResult);
        service.performPluginValidationsFor(packageDefinition);

        assertThat(packageDefinition.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("mandatory field")));
        assertThat(packageDefinition.getConfiguration().get(1).getEncryptedConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("mandatory field")));
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

        when(packageAsRepositoryExtension.isPackageConfigurationValid(eq(packageRepository.getPluginConfiguration().getId()),
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


        PackageDefinitionService service = new PackageDefinitionService(packageAsRepositoryExtension, localizer);

        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageAsRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenReturn(
                new Result().withSuccessMessages("Got Package!!!"));

        service.checkConnection(packageDefinition, result);

        assertPackageConfiguration(packageConfigurationsCaptor.getValue().list(), packageDefinition.getConfiguration());
        assertPackageConfiguration(packageRepositoryConfigurationsCaptor.getValue().list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), Is.is(true));
        when(localizer.localize("PACKAGE_CHECK_OK", "Got Package!!!")).thenReturn("success_msg");
        assertThat(result.message(localizer), Is.is("success_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
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


        PackageDefinitionService service = new PackageDefinitionService(packageAsRepositoryExtension, localizer);
        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageAsRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenReturn(
                new Result().withErrorMessages("Package not available", "Repo not available"));

        service.checkConnection(packageDefinition, result);

        assertPackageConfiguration(packageConfigurationsCaptor.getValue().list(), packageDefinition.getConfiguration());
        assertPackageConfiguration(packageRepositoryConfigurationsCaptor.getValue().list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), Is.is(false));
        when(localizer.localize("PACKAGE_CHECK_FAILED", "Package not available\nRepo not available")).thenReturn("error_msg");
        assertThat(result.message(localizer), Is.is("error_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
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


        PackageDefinitionService service = new PackageDefinitionService(packageAsRepositoryExtension, localizer);


        ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfigurationsCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        ArgumentCaptor<RepositoryConfiguration> packageRepositoryConfigurationsCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageAsRepositoryExtension.checkConnectionToPackage(eq(packageRepository.getPluginConfiguration().getId()),
                packageConfigurationsCaptor.capture(), packageRepositoryConfigurationsCaptor.capture())).thenThrow(
                new RuntimeException("Check connection for package not implemented!!"));

        service.checkConnection(packageDefinition, result);

        assertThat(result.isSuccessful(), Is.is(false));
        when(localizer.localize("PACKAGE_CHECK_FAILED", "Check connection for package not implemented!!")).thenReturn("error_msg");
        assertThat(result.message(localizer), Is.is("error_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToPackage(anyString(), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class));
    }

}

