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

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.update.ConfigUpdateAjaxResponse;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PackageRepositoryServiceTest {

    @Mock
    private PluginManager pluginManager;
    @Mock
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private Localizer localizer;
    @Mock
    private EntityHashingService entityHashingService;
    private PackageRepositoryService service;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new PackageRepositoryService(pluginManager, packageAsRepositoryExtension, goConfigService, securityService, entityHashingService, localizer);
    }

    @Test
    public void shouldSavePackageRepositoryAndReturnSuccess() throws Exception {
        service = spy(service);
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setId("repoid");
        Username username = new Username(new CaseInsensitiveString("user"));
        UpdateConfigFromUI updateConfigFromUI = mock(UpdateConfigFromUI.class);

        doNothing().when(service).performPluginValidationsFor(packageRepository);
        doReturn(updateConfigFromUI).when(service).getPackageRepositoryUpdateCommand(packageRepository, username);

        when(goConfigService.updateConfigFromUI(eq(updateConfigFromUI), eq("md5"), eq(username), any(LocalizedOperationResult.class))).then(new Answer<ConfigUpdateResponse>() {
            @Override
            public ConfigUpdateResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new ConfigUpdateResponse(null, null, null, mock(ConfigAwareUpdate.class), ConfigSaveState.UPDATED);
            }
        });

        when(localizer.localize("SAVED_CONFIGURATION_SUCCESSFULLY")).thenReturn("SAVED_CONFIGURATION_SUCCESSFULLY");

        ConfigUpdateAjaxResponse response = service.savePackageRepositoryToConfig(packageRepository, "md5", username);
        assertThat(response.isSuccessful(), is(true));
        assertThat(response.getMessage(), is("SAVED_CONFIGURATION_SUCCESSFULLY"));
        assertThat(response.getSubjectIdentifier(), is("repoid"));
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));

        verify(service).performPluginValidationsFor(packageRepository);
        verify(service).getPackageRepositoryUpdateCommand(packageRepository, username);
    }

    @Test
    public void shouldFailAndReturnReturnFailureResponse() throws Exception {
        service = spy(service);
        Username username = new Username(new CaseInsensitiveString("user"));

        final PackageRepository packageRepository = new PackageRepository();
        packageRepository.errors().add("name", "Name is invalid");

        final CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("sample");
        cruiseConfig.errors().add("global", "error");

        final UpdateConfigFromUI updateConfigFromUI = mock(UpdateConfigFromUI.class);
        final ConfigAwareUpdate configAwareUpdate = mock(ConfigAwareUpdate.class);

        doNothing().when(service).performPluginValidationsFor(packageRepository);
        doReturn(updateConfigFromUI).when(service).getPackageRepositoryUpdateCommand(packageRepository, username);

        when(configAwareUpdate.configAfter()).thenReturn(cruiseConfig);
        when(goConfigService.updateConfigFromUI(eq(updateConfigFromUI), eq("md5"), eq(username), any(LocalizedOperationResult.class))).then(new Answer<ConfigUpdateResponse>() {
            @Override
            public ConfigUpdateResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
                LocalizedOperationResult result = (LocalizedOperationResult) invocationOnMock.getArguments()[3];
                result.badRequest(LocalizedMessage.string("BAD_REQUEST"));

                return new ConfigUpdateResponse(cruiseConfig, cruiseConfig, packageRepository, configAwareUpdate, ConfigSaveState.UPDATED);
            }
        });
        when(localizer.localize("BAD_REQUEST", new Object[]{})).thenReturn("Save Failed");

        ConfigUpdateAjaxResponse response = service.savePackageRepositoryToConfig(packageRepository, "md5", username);

        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getMessage(), is("Save Failed"));

        assertThat(response.getFieldErrors().size(), is(1));
        assertThat(response.getFieldErrors().get("package_repository[name]"), is(asList("Name is invalid")));

        assertThat(response.getGlobalErrors().size(), is(1));
        assertThat(response.getGlobalErrors().contains("error"), is(true));

        assertThat(response.getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));

        verify(service).performPluginValidationsFor(packageRepository);
        verify(service).getPackageRepositoryUpdateCommand(packageRepository, username);
    }

    @Test
    public void shouldCheckIfUserCanAccessAdminPagesWhileUpdatingPackageRepository() throws Exception {
        Username username = new Username(new CaseInsensitiveString("user"));
        when(securityService.canViewAdminPage(username)).thenReturn(false);

        UpdateConfigFromUI updateCommand = service.getPackageRepositoryUpdateCommand(new PackageRepository(), username);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        updateCommand.checkPermission(GoConfigMother.configWithPipelines("sample"), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(401));
        verify(securityService).canViewAdminPage(username);
    }

    @Test
    public void shouldUpdateSubjectWhenUpdateCalledOnCommand() throws Exception {
        Username username = new Username(new CaseInsensitiveString("user"));
        PackageRepository packageRepository = new PackageRepository();

        UpdateConfigFromUI updateCommand = service.getPackageRepositoryUpdateCommand(packageRepository, username);

        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        updateCommand.update(cruiseConfig);
        verify(cruiseConfig).savePackageRepository(packageRepository);
    }

    @Test
    public void shouldValidateUpdateCommandForPackageRepository() throws Exception {
        Username username = new Username(new CaseInsensitiveString("user"));
        Validatable packageRepository = new PackageRepository();
        ((PackageRepository) packageRepository).setId("id");

        Validatable cruiseConfig = GoConfigMother.configWithPipelines("sample");
        ((CruiseConfig) cruiseConfig).getPackageRepositories().add((PackageRepository) packageRepository);

        UpdateConfigFromUI updateCommand = service.getPackageRepositoryUpdateCommand((PackageRepository) packageRepository, username);

        assertThat(updateCommand.node((CruiseConfig) cruiseConfig), is(cruiseConfig));
        assertThat(updateCommand.updatedNode((CruiseConfig) cruiseConfig), is(cruiseConfig));
        assertThat(updateCommand.subject(cruiseConfig), is(packageRepository));
        assertThat(updateCommand.updatedSubject(cruiseConfig), is(packageRepository));
    }

    @Test
    public void shouldPerformPluginValidationsUsingMetaDataBeforeSavingPackageRepository() throws Exception {
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

        when(packageAsRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId),any(RepositoryConfiguration.class))).thenReturn(new ValidationResult());
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1.0", null, null, null, true));
        when(localizer.localize("MANDATORY_CONFIGURATION_FIELD")).thenReturn("mandatory field");

        service.performPluginValidationsFor(packageRepository);

        assertThat(packageRepository.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("mandatory field")));
        assertThat(packageRepository.getConfiguration().get(1).getEncryptedConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("mandatory field")));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeSavingPackageRepository() throws Exception {
        String pluginId = "yum";
        PackageRepository packageRepository = new PackageRepository();
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, new PackageConfigurations());
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, "1.0"));
        packageRepository.getConfiguration().add(ConfigurationPropertyMother.create("url", false, "junk-url"));

        ArgumentCaptor<RepositoryConfiguration> packageConfigurationsArgumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        ValidationResult expectedValidationResult = new ValidationResult();
        expectedValidationResult.addError(new ValidationError("url", "url format incorrect"));

        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor("yum", "1.0", null, null, null, true));
        when(packageAsRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), packageConfigurationsArgumentCaptor.capture())).thenReturn(expectedValidationResult);

        service = new PackageRepositoryService(pluginManager, packageAsRepositoryExtension, goConfigService, securityService, entityHashingService,mock(Localizer.class));
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getConfiguration().get(0).getConfigurationValue().errors().getAllOn("value"), is(Arrays.asList("url format incorrect")));
    }

    @Test
    public void shouldAddErrorWhenPluginIdIsMissing() {
        PackageRepository packageRepository = new PackageRepository();
        when(localizer.localize("PLUGIN_ID_REQUIRED")).thenReturn("Please provide plugin id");
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().errors().getAllOn(PluginConfiguration.ID), is(Arrays.asList("Please provide plugin id")));
    }

    @Test
    public void shouldAddErrorWhenPluginIdIsInvalid() {
        when(pluginManager.plugins()).thenReturn(Arrays.asList(new GoPluginDescriptor("valid", "1.0", null, null, null, true)));
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration("missing-plugin", "1.0"));
        when(localizer.localize("PLUGIN_ID_INVALID")).thenReturn("Invalid plugin id");
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().errors().getAllOn(PluginConfiguration.ID), is(Arrays.asList("Invalid plugin id")));
    }

    @Test
    public void shouldUpdatePluginVersionWhenValid() {
        String pluginId = "valid";
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, new PackageConfigurations());
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(new GoPluginDescriptor(pluginId, "1.0", null, null, null, true));
        when(packageAsRepositoryExtension.isRepositoryConfigurationValid(eq(pluginId), any(RepositoryConfiguration.class))).thenReturn(new ValidationResult());
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, ""));
        service.performPluginValidationsFor(packageRepository);
        assertThat(packageRepository.getPluginConfiguration().getVersion(), is("1.0"));
    }

    @Test
    public void shouldPerformCheckConnectionOnPlugin() throws Exception {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageAsRepositoryExtension, goConfigService, securityService, entityHashingService, localizer);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        when(packageAsRepositoryExtension.checkConnectionToRepository(eq(pluginId), argumentCaptor.capture())).thenReturn(new Result().withSuccessMessages("Accessed Repo File!!!"));

        service.checkConnection(packageRepository, result);

        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        PackageMaterialTestHelper.assertPackageConfiguration(packageConfigurations.list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), is(true));
        when(localizer.localize("CONNECTION_OK", "Accessed Repo File!!!")).thenReturn("success_msg");
        assertThat(result.message(localizer), is("success_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToRepository(eq(pluginId),any(RepositoryConfiguration.class));
    }

    @Test
    public void shouldPerformCheckConnectionOnPluginAndCatchAnyExceptionsThrownByThePlugin() throws Exception {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageAsRepositoryExtension, goConfigService, securityService, entityHashingService, localizer);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);
        when(packageAsRepositoryExtension.checkConnectionToRepository(eq(pluginId),argumentCaptor.capture())).thenThrow(new RuntimeException("Check Connection not implemented!!"));

        service.checkConnection(packageRepository, result);

        assertThat(result.isSuccessful(), is(false));
        when(localizer.localize("PACKAGE_REPOSITORY_CHECK_CONNECTION_FAILED", "Check Connection not implemented!!")).thenReturn("error_msg");
        assertThat(result.message(localizer), is("error_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToRepository(eq(pluginId),any(RepositoryConfiguration.class));
    }

    @Test
    public void shouldPopulateErrorsForCheckConnectionOnPlugin() throws Exception {
        String pluginId = "yum";
        PackageRepository packageRepository = packageRepository(pluginId);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PackageRepositoryService service = new PackageRepositoryService(pluginManager, packageAsRepositoryExtension, goConfigService, securityService, entityHashingService, localizer);

        ArgumentCaptor<RepositoryConfiguration> argumentCaptor = ArgumentCaptor.forClass(RepositoryConfiguration.class);

        when(packageAsRepositoryExtension.checkConnectionToRepository(eq(pluginId),argumentCaptor.capture())).thenReturn(new Result().withErrorMessages("Repo invalid!!", "Could not connect"));
        service.checkConnection(packageRepository, result);

        RepositoryConfiguration packageConfigurations = argumentCaptor.getValue();
        PackageMaterialTestHelper.assertPackageConfiguration(packageConfigurations.list(), packageRepository.getConfiguration());
        assertThat(result.isSuccessful(), is(false));

        when(localizer.localize("PACKAGE_REPOSITORY_CHECK_CONNECTION_FAILED", "Repo invalid!!\nCould not connect")).thenReturn("error_msg");
        assertThat(result.message(localizer), is("error_msg"));
        verify(packageAsRepositoryExtension).checkConnectionToRepository(eq(pluginId),any(RepositoryConfiguration.class));

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
