/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.thoughtworks.go.helper.MaterialConfigsMother.p4;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyMaterialConfigSerializationTest {

    private MagicalGoConfigXmlWriter writer;
    private MagicalGoConfigXmlLoader loader;

    @BeforeEach
    void setUp() {
        writer = new MagicalGoConfigXmlWriter(ConfigElementImplementationRegistryMother.withNoPlugins());
        loader = new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins());
    }

    @Test
    void shouldBeAbleToLoadADependencyMaterialFromConfig() throws Exception {
        String xml = "<pipeline pipelineName=\"pipeline-name\" stageName=\"stage-name\" />";
        DependencyMaterialConfig material = loader.fromXmlPartial(xml, DependencyMaterialConfig.class);
        assertThat(material.getPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline-name"));
        assertThat(material.getStageName()).isEqualTo(new CaseInsensitiveString("stage-name"));
        assertThat(writer.toXmlPartial(material)).isEqualTo(xml);
    }

    @Test
    void shouldBeAbleToSaveADependencyMaterialToConfig() throws Exception {
        DependencyMaterialConfig originalMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        GoConfigMother goConfigMother = new GoConfigMother();
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        goConfigMother.addPipeline(cruiseConfig, "pipeline-name", "stage-name", "job-name");
        goConfigMother.addPipeline(cruiseConfig, "dependent", "stage-name", "job-name").addMaterialConfig(originalMaterial);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        cruiseConfig.initializeServer();
        writer.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8)).config;

        DependencyMaterialConfig material = (DependencyMaterialConfig) config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs().get(1);
        assertThat(material).isEqualTo(originalMaterial);
        assertThat(material.getPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline-name"));
        assertThat(material.getStageName()).isEqualTo(new CaseInsensitiveString("stage-name"));
    }

    @Test
    void shouldBeAbleToHaveADependencyAndOneOtherMaterial() throws Exception {
        GoConfigMother goConfigMother = new GoConfigMother();
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        goConfigMother.addPipeline(cruiseConfig, "pipeline-name", "stage-name", "job-name");
        MaterialConfig materialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, "dependent", "stage-name", new MaterialConfigs(materialConfig), "job-name");
        pipelineConfig.addMaterialConfig(p4("localhost:1666", "foo"));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        cruiseConfig.initializeServer();

        writer.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8)).config;

        MaterialConfigs materialConfigs = config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs();
        assertThat(materialConfigs.getFirst()).isInstanceOf(DependencyMaterialConfig.class);
        assertThat(materialConfigs.getLast()).isInstanceOf(P4MaterialConfig.class);
    }
}
