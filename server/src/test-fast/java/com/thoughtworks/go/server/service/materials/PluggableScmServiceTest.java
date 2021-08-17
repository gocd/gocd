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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ClearSingleton.class)
public class PluggableScmServiceTest {
    private static final String pluginId = "abc.def";

    @Mock
    private SCMExtension scmExtension;
    @Mock
    private SCMPreference preference;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private SecretParamResolver secretParamResolver;

    private PluggableScmService pluggableScmService;
    private SCMConfigurations scmConfigurations;

    @BeforeEach
    public void setUp() throws Exception {

        pluggableScmService = new PluggableScmService(scmExtension, goConfigService, entityHashingService, secretParamResolver);

        SCMPropertyConfiguration scmConfig = new SCMPropertyConfiguration();
        scmConfig.add(new SCMProperty("KEY1").with(Property.REQUIRED, true));
        scmConfigurations = new SCMConfigurations(scmConfig);

        lenient().when(preference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor(pluginId, preference);
    }

    @Test
    public void shouldValidateSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, false));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertFalse(modifiedSCM.getConfiguration().getProperty("KEY1").errors().isEmpty());
        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().firstError(), is("error message"));
        verify(scmExtension).isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class));
    }

    @Test
    public void shouldHandleIncorrectKeyForValidateSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        configuration.getProperty("KEY1").setConfigurationValue(new ConfigurationValue("junk"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("NON-EXISTENT-KEY", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertTrue(modifiedSCM.errors().isEmpty());
    }

    @Test
    public void shouldValidateMandatoryFieldsForSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertFalse(validationErrors.isEmpty());
        final ValidationError validationError = getValidationErrorFor(validationErrors, "KEY1");
        assertNotNull(validationError);
        assertThat(validationError.getMessage(), is("This field is required"));
    }

    @Test
    public void shouldValidateMandatoryAndSecureFieldsForSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, true).with(Property.SECURE, true));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("KEY2", true, ""));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertFalse(validationErrors.isEmpty());
        final ValidationError validationErrorForKey1 = getValidationErrorFor(validationErrors, "KEY1");
        assertNotNull(validationErrorForKey1);
        assertThat(validationErrorForKey1.getMessage(), is("This field is required"));
        final ValidationError validationErrorForKey2 = getValidationErrorFor(validationErrors, "KEY2");
        assertNotNull(validationErrorForKey2);
        assertThat(validationErrorForKey2.getMessage(), is("This field is required"));
    }

    @Test
    public void shouldPassValidationIfAllRequiredFieldsHaveValuesForSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        configuration.getProperty("KEY1").setConfigurationValue(new ConfigurationValue("junk"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        pluggableScmService.validate(modifiedSCM);

        assertTrue(validationResult.getErrors().isEmpty());
    }

    @Test
    public void shouldCallPluginToCheckConnectionForTheGivenSCMConfiguration() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withSuccessMessages(singletonList("message"));

        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(resultFromPlugin);

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Connection OK. message"));
    }

    @Test
    public void checkConnectionResultShouldFailForFailureResponseFromPlugin() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withErrorMessages("connection failed");

        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(resultFromPlugin);

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertThat(result.httpCode(), is(422));
        assertThat(result.message(), is("Check connection failed. Reason(s): connection failed"));
    }

    @Test
    public void shouldReturnAListOfAllScmsInTheConfig() {
        SCMs list = new SCMs();
        list.add(new SCM());
        when(goConfigService.getSCMs()).thenReturn(list);

        SCMs scms = pluggableScmService.listAllScms();

        assertThat(scms, is(list));
    }

    @Test
    public void shouldReturnAPluggableScmMaterialIfItExists() {
        SCM scm = new SCM("1", null, null);
        scm.setName("foo");

        SCMs list = new SCMs();
        list.add(scm);
        when(goConfigService.getSCMs()).thenReturn(list);

        assertThat(pluggableScmService.findPluggableScmMaterial("foo"), is(scm));
    }

    @Test
    public void shouldReturnNullIfPluggableScmMaterialDoesNotExist() {
        SCMs scms = new SCMs();
        when(goConfigService.getSCMs()).thenReturn(scms);

        assertNull(pluggableScmService.findPluggableScmMaterial("bar"));
    }

    @Test
    public void shouldDeleteSCMConfigIfValid() {
        doNothing().when(goConfigService).updateConfig(any(), any());
        SCM scm = new SCM("id", "name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pluggableScmService.deletePluggableSCM(new Username("admin"), scm, result);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void isValidShouldSkipValidationAgainstPluginIfPluginIsNonExistent() {
        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(false);

        assertThatThrownBy(() -> pluggableScmService.isValid(scmConfig))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(scmExtension);
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToPluggableSCMConfigurations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("url", false, "url"));
        configuration.add(ConfigurationPropertyMother.create("username", false, "admin"));

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("url", "invalid"));
        validationResult.addError(new ValidationError("username", "invalid"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(configuration);
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        assertFalse(pluggableScmService.isValid(scmConfig));
        assertThat(configuration.getProperty("url").errors().get("url").get(0), is("invalid"));
        assertThat(configuration.getProperty("username").errors().get("username").get(0), is("invalid"));
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToPluggableSCMForMissingConfigurations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("url", "URL is a required field"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(new Configuration());
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);

        assertFalse(pluggableScmService.isValid(scmConfig));
        verify(scmConfig).addError("url", "URL is a required field");
    }

    @Test
    public void shouldSendResolvedValueToPluginDuringValidateSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, false));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1", "{{SECRET:[secret_config_id][lookup_username]}}"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);
        doAnswer(invocation -> {
            SCM config = invocation.getArgument(0);
            config.getSecretParams().get(0).setValue("resolved-value");
            return config;
        }).when(secretParamResolver).resolve(modifiedSCM);

        pluggableScmService.validate(modifiedSCM);

        verify(secretParamResolver).resolve(modifiedSCM);
        assertFalse(modifiedSCM.getConfiguration().getProperty("KEY1").errors().isEmpty());
        assertThat(modifiedSCM.getConfiguration().getProperty("KEY1").errors().firstError(), is("error message"));
        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        verify(scmExtension).isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), captor.capture());
        assertThat(captor.getValue().list().get(0).getValue(), is("resolved-value"));
    }

    @Test
    public void shouldSendResolvedValueToPluginDuringIsValidCall() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("url", false, "url"));
        configuration.add(ConfigurationPropertyMother.create("username", false, "{{SECRET:[secret_config_id][username]}}"));

        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(true);
        when(scmConfig.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(scmConfig.getConfiguration()).thenReturn(configuration);
        when(scmExtension.isSCMConfigurationValid(any(String.class), any(SCMPropertyConfiguration.class))).thenReturn(new ValidationResult());
        doAnswer(invocation -> {
            configuration.get(1).getSecretParams().get(0).setValue("resolved-value");
            return scmConfig;
        }).when(secretParamResolver).resolve(any(SCM.class));

        assertTrue(pluggableScmService.isValid(scmConfig));

        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        verify(scmExtension).isSCMConfigurationValid(anyString(), captor.capture());
        assertThat(captor.getValue().list().get(1).getValue(), is("resolved-value"));
    }

    @Test
    public void shouldCallPluginAndSendResolvedValuesToCheckConnectionForTheGivenSCMConfiguration() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1", "{{SECRET:[secret_config_id][value]}}"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result resultFromPlugin = new Result();
        resultFromPlugin.withSuccessMessages(singletonList("message"));

        ArgumentCaptor<SCMPropertyConfiguration> captor = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), captor.capture())).thenReturn(resultFromPlugin);
        doAnswer(invocation -> {
            configuration.get(0).getSecretParams().get(0).setValue("resolved-value");
            return modifiedSCM;
        }).when(secretParamResolver).resolve(any(SCM.class));

        HttpLocalizedOperationResult result = pluggableScmService.checkConnection(modifiedSCM);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Connection OK. message"));
        assertThat(captor.getValue().list().get(0).getValue(), is("resolved-value"));
    }

    private ValidationError getValidationErrorFor(List<ValidationError> validationErrors, final String key) {
        return validationErrors.stream().filter(new Predicate<ValidationError>() {
            @Override
            public boolean test(ValidationError item) {
                return item.getKey().equals(key);
            }
        }).findFirst().orElse(null);
    }
}
