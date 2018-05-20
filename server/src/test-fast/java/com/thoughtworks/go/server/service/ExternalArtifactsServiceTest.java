/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class ExternalArtifactsServiceTest {
    private PluggableArtifactConfig pluggableArtifactConfig;
    private ExternalArtifactsService externalArtifactsService;
    private ArtifactExtension artifactExtension;
    private BasicCruiseConfig cruiseConfig;
    private String pluginId = "abc.def";

    @Before
    public void setUp() {
        artifactExtension = mock(ArtifactExtension.class);
        externalArtifactsService = new ExternalArtifactsService(artifactExtension);
        ArtifactPluginInfo pluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn(pluginId);
        ArtifactMetadataStore.instance().setPluginInfo(pluginInfo);

        pluggableArtifactConfig = new PluggableArtifactConfig("foo", "bar");

        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("bar", pluginId));

    }

    @Test
    public void shouldValidateExternalArtifactConfig() {
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig);
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("Image", false, "foo"));
        pluggableArtifactConfig.setConfiguration(configuration);

        when(artifactExtension.validatePluggableArtifactConfig(any(), eq(configuration.getConfigurationAsMap(true)))).thenReturn(new ValidationResult());
        externalArtifactsService.validate(pluggableArtifactConfig, mock(ArtifactStore.class), validationContext);

        assertFalse(pluggableArtifactConfig.hasErrors());
    }

    @Test
    public void shouldSkipValidationAgainstPluginIfExternalArtifactIsInvalid() {
        cruiseConfig.getArtifactStores().clear();

        externalArtifactsService.validate(pluggableArtifactConfig, mock(ArtifactStore.class), PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig));

        verifyZeroInteractions(artifactExtension);
    }

    @Test
    public void shouldMapPluginValidationErrorsToExternalArtifactConfigrations() {
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

        externalArtifactsService.validate(pluggableArtifactConfig, artifactStore, PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig));

        assertThat(configuration.getProperty("Image").errors().get("Image").get(0), is("invalid"));
        assertThat(configuration.getProperty("Tag").errors().get("Tag").get(0), is("invalid"));
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToExternalArtifactForMissingConfigurations() {
        pluggableArtifactConfig.getConfiguration().clear();

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("configuration", "Either Image or BuildFile is required"));


        when(artifactExtension.validatePluggableArtifactConfig(any(), any())).thenReturn(validationResult);

        externalArtifactsService.validate(pluggableArtifactConfig, mock(ArtifactStore.class), PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig));

        assertThat(pluggableArtifactConfig.errors().getAllOn("configuration").get(0), is("Either Image or BuildFile is required"));
    }
}
