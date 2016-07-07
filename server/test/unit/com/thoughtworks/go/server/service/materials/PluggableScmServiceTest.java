/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.ListUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableScmServiceTest {
    private static final String pluginId = "abc.def";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private SCMExtension scmExtension;
    @Mock
    private Localizer localizer;
    @Mock
    private SCMPreference preference;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService entityHashingService;

    private PluggableScmService pluggableScmService;
    private SCMConfigurations scmConfigurations;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        pluggableScmService = new PluggableScmService(scmExtension, localizer, goConfigService, entityHashingService);

        SCMPropertyConfiguration scmConfig = new SCMPropertyConfiguration();
        scmConfig.add(new SCMProperty("KEY1").with(Property.REQUIRED, true));
        scmConfigurations = new SCMConfigurations(scmConfig);

        when(preference.getScmConfigurations()).thenReturn(scmConfigurations);
        SCMMetadataStore.getInstance().setPreferenceFor(pluginId, preference);
    }

    @After
    public void teardown() {
        SCMMetadataStore.getInstance().clear();
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
        when(localizer.localize("MANDATORY_CONFIGURATION_FIELD")).thenReturn("MANDATORY_CONFIGURATION_FIELD");

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertFalse(validationErrors.isEmpty());
        final ValidationError validationError = getValidationErrorFor(validationErrors, "KEY1");
        assertNotNull(validationError);
        assertThat(validationError.getMessage(), is("MANDATORY_CONFIGURATION_FIELD"));
    }

    @Test
    public void shouldValidateMandatoryAndSecureFieldsForSCM() {
        SCMConfiguration scmConfig = new SCMConfiguration(new SCMProperty("KEY2").with(Property.REQUIRED, true).with(Property.SECURE, true));
        scmConfigurations.add(scmConfig);

        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("KEY2", true, ""));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(scmExtension.isSCMConfigurationValid(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(validationResult);
        when(localizer.localize("MANDATORY_CONFIGURATION_FIELD")).thenReturn("MANDATORY_CONFIGURATION_FIELD");

        pluggableScmService.validate(modifiedSCM);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertFalse(validationErrors.isEmpty());
        final ValidationError validationErrorForKey1 = getValidationErrorFor(validationErrors, "KEY1");
        assertNotNull(validationErrorForKey1);
        assertThat(validationErrorForKey1.getMessage(), is("MANDATORY_CONFIGURATION_FIELD"));
        final ValidationError validationErrorForKey2 = getValidationErrorFor(validationErrors, "KEY2");
        assertNotNull(validationErrorForKey2);
        assertThat(validationErrorForKey2.getMessage(), is("MANDATORY_CONFIGURATION_FIELD"));
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
    public void shouldCheckConnectionToSCM() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        SCM modifiedSCM = new SCM("scm-id", new PluginConfiguration(pluginId, "1"), configuration);
        Result expectedResult = new Result();
        expectedResult.withSuccessMessages(Arrays.asList("message"));
        when(scmExtension.checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class))).thenReturn(expectedResult);

        Result gotResult = pluggableScmService.checkConnection(modifiedSCM);

        verify(scmExtension).checkConnectionToSCM(eq(modifiedSCM.getPluginConfiguration().getId()), any(SCMPropertyConfiguration.class));
        assertSame(expectedResult, gotResult);
    }

    @Test
    public void shouldReturnAListOfAllScmsInTheConfig() {
        ArrayList<SCM> list = new ArrayList<>();
        list.add(new SCM());
        when(goConfigService.getSCMs()).thenReturn(list);

        ArrayList<SCM> scms = pluggableScmService.listAllScms();

        assertThat(scms, is(list));
    }

    @Test
    public void shouldReturnAPluggableScmMaterialIfItExists() {
        SCM scm = new SCM("1", null, null);
        scm.setName("foo");

        ArrayList<SCM> list = new ArrayList<>();
        list.add(scm);
        when(goConfigService.getSCMs()).thenReturn(list);

        assertThat(pluggableScmService.findPluggableScmMaterial("foo"), is(scm));
    }

    @Test
    public void shouldReturnNullIfPluggableScmMaterialDoesNotExist() {
        ArrayList<SCM> scms = new ArrayList<>();
        when(goConfigService.getSCMs()).thenReturn(scms);

        assertNull(pluggableScmService.findPluggableScmMaterial("bar"));
    }

    @Test
    public void isValidShouldSkipValidationAgainstPluginIfPluginIsNonExistent() {
        SCM scmConfig = mock(SCM.class);

        when(scmConfig.doesPluginExist()).thenReturn(false);

        thrown.expect(RuntimeException.class);
        pluggableScmService.isValid(scmConfig);

        verifyZeroInteractions(scmExtension);
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



    private ValidationError getValidationErrorFor(List<ValidationError> validationErrors, final String key) {
        return ListUtil.find(validationErrors, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ValidationError) item).getKey().equals(key);
            }
        });
    }
}
