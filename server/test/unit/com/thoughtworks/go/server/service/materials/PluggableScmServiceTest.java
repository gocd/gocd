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
import com.thoughtworks.go.util.ListUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableScmServiceTest {
    private static final String pluginId = "abc.def";

    @Mock
    private SCMExtension scmExtension;
    @Mock
    private Localizer localizer;
    @Mock
    private SCMPreference preference;

    private PluggableScmService pluggableScmService;
    private SCMConfigurations scmConfigurations;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        pluggableScmService = new PluggableScmService(scmExtension, localizer);

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

    private ValidationError getValidationErrorFor(List<ValidationError> validationErrors, final String key) {
        return ListUtil.find(validationErrors, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ValidationError) item).getKey().equals(key);
            }
        });
    }
}
