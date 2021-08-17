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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


public class ExternalArtifactsServiceTest {
    private PluggableArtifactConfig pluggableArtifactConfig;
    private FetchPluggableArtifactTask fetchPluggableArtifactTask;
    private PipelineConfig pipelineConfig;
    private ExternalArtifactsService externalArtifactsService;
    private ArtifactExtension artifactExtension;
    private BasicCruiseConfig cruiseConfig;
    private String pluginId = "abc.def";

    @BeforeEach
    public void setUp() {
        artifactExtension = mock(ArtifactExtension.class);
        externalArtifactsService = new ExternalArtifactsService(artifactExtension);
        ArtifactPluginInfo pluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn(pluginId);
        ArtifactMetadataStore.instance().setPluginInfo(pluginInfo);

        pluggableArtifactConfig = new PluggableArtifactConfig("foo", "bar");
        pipelineConfig = PipelineConfigMother.createPipelineConfig("p1", "s1", "j1");
        pipelineConfig.getStage("s1").jobConfigByConfigName("j1").artifactTypeConfigs().add(pluggableArtifactConfig);
        fetchPluggableArtifactTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1"), new CaseInsensitiveString("j1"), "foo");

        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);
        cruiseConfig.getArtifactStores().add(new ArtifactStore("bar", pluginId));

    }

    @Test
    public void shouldValidateExternalArtifactConfig() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("Image", false, "foo"));
        pluggableArtifactConfig.setConfiguration(configuration);
        ArtifactStore artifactStore = mock(ArtifactStore.class);
        when(artifactStore.getPluginId()).thenReturn(pluginId);
        when(artifactExtension.validatePluggableArtifactConfig(any(), eq(configuration.getConfigurationAsMap(true)))).thenReturn(new ValidationResult());

        externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, artifactStore, true);

        assertFalse(pluggableArtifactConfig.hasErrors());
    }

    @Test
    public void shouldValidateFetchArtifactTaskConfig() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("Dest", false, "foo"));

        fetchPluggableArtifactTask.setConfiguration(configuration);
        when(artifactExtension.validateFetchArtifactConfig(any(), eq(configuration.getConfigurationAsMap(true)))).thenReturn(new ValidationResult());

        externalArtifactsService.validateFetchExternalArtifactTask(fetchPluggableArtifactTask, pipelineConfig, cruiseConfig);
        assertTrue(fetchPluggableArtifactTask.errors().isEmpty());
    }

    @Test
    public void shouldSkipValidationAgainstPluginIfExternalArtifactIsInvalid() {
        cruiseConfig.getArtifactStores().clear();

        externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, mock(ArtifactStore.class), true);

        verifyNoInteractions(artifactExtension);
    }

    @Test
    public void shouldSkipValidationAgainstPluginIfFetchExternalArtifactIsInvalid() {
        cruiseConfig.getArtifactStores().clear();

        externalArtifactsService.validateFetchExternalArtifactTask(fetchPluggableArtifactTask, pipelineConfig, cruiseConfig);

        verifyNoInteractions(artifactExtension);
    }

    @Test
    public void shouldAddErrorWhenPluggableArtifactDoesNotHaveValidStore() {
        cruiseConfig.getArtifactStores().clear();

        externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, mock(ArtifactStore.class), true);

        verifyNoInteractions(artifactExtension);

        assertTrue(pluggableArtifactConfig.hasErrors());
        assertThat(pluggableArtifactConfig.errors().getAllOn("pluginId"), is(Arrays.asList("Could not determine the plugin to perform the plugin validations. Possible reasons: artifact store does not exist or plugin is not installed.")));
    }

    @Test
    public void shouldMapPluginValidationErrorsToConfigrationProperties() {
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("Image", false, "alpine"));
        configuration.add(ConfigurationPropertyMother.create("Tag", false, "fml"));

        pluggableArtifactConfig.setConfiguration(configuration);

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("Image", "invalid"));
        validationResult.addError(new ValidationError("Tag", "invalid"));
        ArtifactStore artifactStore = mock(ArtifactStore.class);
        when(artifactStore.getPluginId()).thenReturn(pluginId);

        when(artifactExtension.validatePluggableArtifactConfig(any(String.class), any())).thenReturn(validationResult);

        externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, artifactStore, true);

        assertThat(configuration.getProperty("Image").errors().get("Image").get(0), is("invalid"));
        assertThat(configuration.getProperty("Tag").errors().get("Tag").get(0), is("invalid"));
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToExternalArtifactForMissingConfigurations() {
        pluggableArtifactConfig.getConfiguration().clear();

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("configuration", "Either Image or BuildFile is required"));

        ArtifactStore artifactStore = mock(ArtifactStore.class);
        when(artifactStore.getPluginId()).thenReturn(pluginId);
        when(artifactExtension.validatePluggableArtifactConfig(any(), any())).thenReturn(validationResult);

        externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, artifactStore, true);

        assertThat(pluggableArtifactConfig.errors().getAllOn("configuration").get(0), is("Either Image or BuildFile is required"));
    }
}
