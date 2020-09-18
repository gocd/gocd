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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class PackageRepositoryServiceTest {

    @Mock
    private PluginManager pluginManager;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private EntityHashingService entityHashingService;
    private PackageRepositoryService service;
    @Mock
    private SecretParamResolver secretParamResolver;

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @BeforeEach
    void setUp() {
        initMocks(this);
        service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);
    }

    @Test
    void shouldPerformPluginValidationsUsingMetaDataBeforeSavingPackageRepository() {
        //metadata setup
        String pluginId = "yum";

        PackageConfigurations repositoryConfiguration = new PackageConfigurations();
        repositoryConfiguration.add(new PackageConfiguration("required").with(PackageConfiguration.REQUIRED, true));
        repositoryConfiguration.add(new PackageConfiguration("required_secure").with(PackageConfiguration.REQUIRED, true).with(PackageConfiguration.SECURE, true));
        repositoryConfiguration.add(new PackageConfiguration("secure").with(PackageConfiguration.SECURE, true).with(PackageConfiguration.REQUIRED, false));
        repositoryConfiguration.add(new PackageConfiguration("not_required_not_secure"));
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, repositoryConfiguration);

        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, "1.0"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("required", false, ""));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("required_secure", true, ""));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("secure", true, ""));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("not_required_not_secure", false, ""));

        when(packageRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), any(RepositoryConfiguration.class))).thenReturn(new ValidationResult());
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(getPluginDescriptor(pluginId));

        service.performPluginValidationsFor(packageRepository);

        assertThat(packageRepository.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value")).isEqualTo(Arrays.asList("This field is required"));
        assertThat(packageRepository.getConfiguration().get(1).getEncryptedConfigurationValue().errors().getAllOn("value")).isEqualTo(Arrays.asList("This field is required"));
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId) {
        return GoPluginDescriptor.builder().id(pluginId).version("1.0").isBundledPlugin(true).build();
    }

    @Test
    void shouldInvokePluginValidationsBeforeSavingPackageRepository() {
        String pluginId = "yum";
        PackageRepository packageRepository = new PackageRepository();
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, new PackageConfigurations());
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, "1.0"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("url", false, "junk-url"));

        ArgumentCaptor<RepositoryConfiguration> packageConfigurationsArgumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        ValidationResult expectedValidationResult = new ValidationResult();
        expectedValidationResult.addError(new ValidationError("url", "url format incorrect"));

        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(getPluginDescriptor("yum"));
        when(packageRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), packageConfigurationsArgumentCaptor.capture())).thenReturn(expectedValidationResult);

        service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value")).isEqualTo(Arrays.asList("url format incorrect"));
    }

    @Test
    void shouldAddErrorWhenPluginIdIsMissing() {
        PackageRepository packageRepository = new PackageRepository();
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().errors().getAllOn(PluginConfiguration.ID)).isEqualTo(Arrays.asList("Please select package repository plugin"));
    }

    @Test
    void shouldAddErrorWhenPluginIdIsInvalid() {
        when(pluginManager.plugins()).thenReturn(Arrays.asList(getPluginDescriptor("valid")));
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration("missing-plugin", "1.0"));
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().errors().getAllOn(PluginConfiguration.ID)).isEqualTo(Arrays.asList("Invalid plugin id"));
    }

    @Test
    void shouldUpdatePluginVersionWhenValid() {
        String pluginId = "valid";
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, new PackageConfigurations());
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(getPluginDescriptor(pluginId));
        when(packageRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), any(RepositoryConfiguration.class))).thenReturn(new ValidationResult());
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, ""));
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().getVersion()).isEqualTo("1.0");
    }

    @Test
    void shouldPerformCheckConnectionOnPlugin() {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        when(packageRepositoryExtension.checkConnectionToRepository(eq(pluginId), argumentCaptor.capture())).thenReturn(new Result().withSuccessMessages("Accessed Repo File!!!"));

        service.checkConnection(packageRepository, result);

        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        PackageMaterialTestHelper.assertPackageConfiguration(packageConfigurations.list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Connection OK. Accessed Repo File!!!");
        verify(packageRepositoryExtension).checkConnectionToRepository(eq(pluginId), any(RepositoryConfiguration.class));
    }

    @Test
    void shouldPerformCheckConnectionOnPluginAndCatchAnyExceptionsThrownByThePlugin() {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        when(packageRepositoryExtension.checkConnectionToRepository(eq(pluginId), argumentCaptor.capture())).thenThrow(new RuntimeException("Check Connection not implemented!!"));

        service.checkConnection(packageRepository, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Could not connect to package repository. Reason(s): Check Connection not implemented!!");
        verify(packageRepositoryExtension).checkConnectionToRepository(eq(pluginId), any(RepositoryConfiguration.class));
    }

    @Test
    void shouldPopulateErrorsForCheckConnectionOnPlugin() {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageRepositoryExtension.checkConnectionToRepository(eq(pluginId), argumentCaptor.capture())).thenReturn(new Result().withErrorMessages("Repo invalid!!", "Could not connect"));
        service.checkConnection(packageRepository, result);

        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        PackageMaterialTestHelper.assertPackageConfiguration(packageConfigurations.list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful()).isFalse();

        assertThat(result.message()).isEqualTo("Could not connect to package repository. Reason(s): Repo invalid!!\nCould not connect");
        verify(packageRepositoryExtension).checkConnectionToRepository(eq(pluginId), any(RepositoryConfiguration.class));
    }

    @Test
    void shouldSendResolvedValuesForCheckConnectionOnPlugin() {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        packageRepository.getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][username]}}"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);

        when(packageRepositoryExtension.checkConnectionToRepository(eq(pluginId), any())).thenReturn(new Result().withErrorMessages("Repo invalid!!", "Could not connect"));
        doAnswer(invocation -> {
            PackageRepository config = invocation.getArgument(0);
            config.getSecretParams().get(0).setValue("resolved-value");
            return config;
        }).when(secretParamResolver).resolve(packageRepository);

        service.checkConnection(packageRepository, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Could not connect to package repository. Reason(s): Repo invalid!!\nCould not connect");

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        verify(packageRepositoryExtension).checkConnectionToRepository(eq(pluginId), argumentCaptor.capture());
        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        assertThat(packageConfigurations.list().get(0).getValue()).isEqualTo("resolved-value");

    }

    @Test
    void shouldSendResolvedValuesForValidateRepositoryConfigurationOnPlugin() {
        String pluginId = "yum";
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, new PackageConfigurations());
        PackageRepository packageRepository = packageRepository(pluginId);
        packageRepository.getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][username]}}"));
        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageRepositoryExtension, goConfigService, securityService, entityHashingService, secretParamResolver);

        when(packageRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), any())).thenReturn(new ValidationResult());
        doAnswer(invocation -> {
            PackageRepository config = invocation.getArgument(0);
            config.getSecretParams().get(0).setValue("resolved-value");
            return config;
        }).when(secretParamResolver).resolve(packageRepository);

        boolean isValid = service.validateRepositoryConfiguration(packageRepository);

        assertThat(isValid).isTrue();
        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        verify(packageRepositoryExtension).isRepositoryConfigurationValid(eq(pluginId), argumentCaptor.capture());
        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        assertThat(packageConfigurations.list().get(0).getValue()).isEqualTo("resolved-value");

    }

    @Test
    void shouldSetResultAsUnprocessableEntityIfRulesViolationForUpdate() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        doThrow(new RulesViolationException("some rule violation message")).when(goConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        service.createPackageRepository(packageRepository("some-plugin"), new Username("user"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Save failed. some rule violation message");
    }

    @Test
    void shouldSetResultAsUnprocessableEntityIfSecretResolutionFailsForUpdate() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        doThrow(new SecretResolutionFailureException("some secret resolution message")).when(goConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        service.createPackageRepository(packageRepository("some-plugin"), new Username("user"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Save failed. some secret resolution message");
    }

    @Test
    void shouldSetResultAsUnprocessableEntityIfRulesViolationForCheckConnection() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        doThrow(new RulesViolationException("some rule violation message")).when(secretParamResolver).resolve(any(PackageRepository.class));

        service.checkConnection(packageRepository("some-plugin"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Could not connect to package repository. Reason(s): some rule violation message");
    }

    @Test
    void shouldSetResultAsUnprocessableEntityIfSecretResolutionFailsForCheckConnection() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        doThrow(new SecretResolutionFailureException("some secret resolution message")).when(secretParamResolver).resolve(any(PackageRepository.class));

        service.checkConnection(packageRepository("some-plugin"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        assertThat(result.message()).isEqualTo("Could not connect to package repository. Reason(s): some secret resolution message");
    }

    private PackageRepository packageRepository(String pluginId) {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, "1.0"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("p1", false, "v1"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("p2", true, "v2"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("p3", true, "v3"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("p4", false, "v4"));
        return packageRepository;
    }
}
