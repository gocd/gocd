/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.BuildArtifactConfig.DEST;
import static com.thoughtworks.go.config.BuildArtifactConfig.SRC;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactTypeConfigsTest {
    @Test
    public void shouldAddDuplicatedArtifactSoThatValidationKicksIn() throws Exception {
        final ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        assertThat(artifactTypeConfigs.size(), is(0));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));
        assertThat(artifactTypeConfigs.size(), is(2));
    }

    @Test
    public void shouldLoadArtifactPlans() {
        HashMap<String, String> artifactPlan1 = new HashMap<>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactConfig.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", BuildArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan2);

        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactTypeConfigs.size(), is(2));
        TestArtifactConfig plan = new TestArtifactConfig();
        plan.setSource("blah");
        plan.setDestination("something");
        assertThat(artifactTypeConfigs.get(0), is(plan));
        assertThat(artifactTypeConfigs.get(1), is(new BuildArtifactConfig("blah2", "something2")));
    }

    @Test
    public void setConfigAttributes_shouldIgnoreEmptySourceAndDest() {
        HashMap<String, String> artifactPlan1 = new HashMap<>();
        artifactPlan1.put(SRC, "blah");
        artifactPlan1.put(DEST, "something");
        artifactPlan1.put("artifactTypeValue", TestArtifactConfig.TEST_PLAN_DISPLAY_NAME);
        HashMap<String, String> artifactPlan2 = new HashMap<>();
        artifactPlan2.put(SRC, "blah2");
        artifactPlan2.put(DEST, "something2");
        artifactPlan2.put("artifactTypeValue", BuildArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        HashMap<String, String> artifactPlan3 = new HashMap<>();
        artifactPlan3.put(SRC, "");
        artifactPlan3.put(DEST, "");
        artifactPlan3.put("artifactTypeValue", BuildArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME);

        List<HashMap> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);
        artifactPlansList.add(artifactPlan3);
        artifactPlansList.add(artifactPlan2);

        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactTypeConfigs.size(), is(2));
        TestArtifactConfig plan = new TestArtifactConfig();
        plan.setSource("blah");
        plan.setDestination("something");
        assertThat(artifactTypeConfigs.get(0), is(plan));
        assertThat(artifactTypeConfigs.get(1), is(new BuildArtifactConfig("blah2", "something2")));
    }

    @Test
    public void setConfigAttributes_shouldSetExternalArtifactWithPlainTextValuesIfPluginIdIsProvided() {
        ArtifactPluginInfo artifactPluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(artifactPluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn("cd.go.artifact.foo");
        PluginConfiguration image = new PluginConfiguration("Image", new Metadata(true, true));
        PluginConfiguration tag = new PluginConfiguration("Tag", new Metadata(true, false));
        ArrayList<PluginConfiguration> pluginMetadata = new ArrayList<>();
        pluginMetadata.add(image);
        pluginMetadata.add(tag);
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginMetadata));
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);


        HashMap<Object, Object> configurationMap1 = new HashMap<>();
        configurationMap1.put("Image", "gocd/gocd-server");
        configurationMap1.put("Tag", "v18.6.0");

        HashMap<String, Object> artifactPlan1 = new HashMap<>();
        artifactPlan1.put("artifactTypeValue", "Pluggable Artifact");
        artifactPlan1.put("id", "artifactId");
        artifactPlan1.put("storeId", "storeId");
        artifactPlan1.put("pluginId", "cd.go.artifact.foo");
        artifactPlan1.put("configuration", configurationMap1);

        List<Map> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);

        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactTypeConfigs.size(), is(1));

        PluggableArtifactConfig artifactConfig = (PluggableArtifactConfig) artifactTypeConfigs.get(0);
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.external));
        assertThat(artifactConfig.getId(), is("artifactId"));
        assertThat(artifactConfig.getStoreId(), is("storeId"));
        assertThat(artifactConfig.getConfiguration().getProperty("Image").isSecure(), is(false));
    }

    @Test
    public void setConfigAttributes_shouldSetConfigurationAsIsIfPluginIdIsBlank() throws CryptoException {
        HashMap<Object, Object> imageMap = new HashMap<>();
        imageMap.put("value", new GoCipher().encrypt("some-encrypted-value"));
        imageMap.put("isSecure", "true");

        HashMap<Object, Object> tagMap = new HashMap<>();
        tagMap.put("value", "18.6.0");
        tagMap.put("isSecure", "false");

        HashMap<Object, Object> configurationMap1 = new HashMap<>();
        configurationMap1.put("Image", imageMap);
        configurationMap1.put("Tag", tagMap);

        HashMap<String, Object> artifactPlan1 = new HashMap<>();
        artifactPlan1.put("artifactTypeValue", "Pluggable Artifact");
        artifactPlan1.put("id", "artifactId");
        artifactPlan1.put("storeId", "storeId");
        artifactPlan1.put("pluginId", "");
        artifactPlan1.put("configuration", configurationMap1);

        List<Map> artifactPlansList = new ArrayList<>();
        artifactPlansList.add(artifactPlan1);

        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.setConfigAttributes(artifactPlansList);

        assertThat(artifactTypeConfigs.size(), is(1));

        PluggableArtifactConfig artifactConfig = (PluggableArtifactConfig) artifactTypeConfigs.get(0);
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.external));
        assertThat(artifactConfig.getId(), is("artifactId"));
        assertThat(artifactConfig.getStoreId(), is("storeId"));
        assertThat(artifactConfig.getConfiguration().getProperty("Image").getValue(), is("some-encrypted-value"));
        assertThat(artifactConfig.getConfiguration().getProperty("Tag").getValue(), is("18.6.0"));
    }

    @Test
    public void shouldClearAllArtifactsWhenTheMapIsNull() {
        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));

        artifactTypeConfigs.setConfigAttributes(null);

        assertThat(artifactTypeConfigs.size(), is(0));
    }

    @Test
    public void shouldValidateTree() {
        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "../a"));

        artifactTypeConfigs.validateTree(null);
        assertThat(artifactTypeConfigs.get(0).errors().on(BuiltinArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(0).errors().on(BuiltinArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(1).errors().on(BuiltinArtifactConfig.DEST), is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(1).errors().on(BuiltinArtifactConfig.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(2).errors().on(BuiltinArtifactConfig.DEST), is("Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN));
    }

    @Test
    public void shouldErrorOutWhenDuplicateArtifactConfigExists() {
        final ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs(new BuildArtifactConfig("src", "dest"));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));
        artifactTypeConfigs.add(new BuildArtifactConfig("src", "dest"));

        artifactTypeConfigs.validate(null);

        assertFalse(artifactTypeConfigs.get(0).errors().isEmpty());
        assertThat(artifactTypeConfigs.get(0).errors().on(BuiltinArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(0).errors().on(BuiltinArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));

        assertFalse(artifactTypeConfigs.get(1).errors().isEmpty());
        assertThat(artifactTypeConfigs.get(1).errors().on(BuiltinArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(1).errors().on(BuiltinArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));

        assertFalse(artifactTypeConfigs.get(2).errors().isEmpty());
        assertThat(artifactTypeConfigs.get(2).errors().on(BuiltinArtifactConfig.SRC), Matchers.is("Duplicate artifacts defined."));
        assertThat(artifactTypeConfigs.get(2).errors().on(BuiltinArtifactConfig.DEST), Matchers.is("Duplicate artifacts defined."));
    }

    @Test
    public void getArtifactConfigs_shouldReturnBuiltinArtifactConfigs() {
        ArtifactTypeConfigs allConfigs = new ArtifactTypeConfigs();
        allConfigs.add(new BuildArtifactConfig("src", "dest"));
        allConfigs.add(new BuildArtifactConfig("java", null));
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final List<BuiltinArtifactConfig> artifactConfigs = allConfigs.getBuiltInArtifactConfigs();

        assertThat(artifactConfigs, hasSize(2));
        assertThat(artifactConfigs, containsInAnyOrder(
                new BuildArtifactConfig("src", "dest"),
                new BuildArtifactConfig("java", null)
        ));
    }

    @Test
    public void getPluggableArtifactConfigs_shouldReturnPluggableArtifactConfigs() {
        ArtifactTypeConfigs allConfigs = new ArtifactTypeConfigs();
        allConfigs.add(new BuildArtifactConfig("src", "dest"));
        allConfigs.add(new BuildArtifactConfig("java", null));
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final List<PluggableArtifactConfig> artifactConfigs = allConfigs.getPluggableArtifactConfigs();

        assertThat(artifactConfigs, hasSize(2));
        assertThat(artifactConfigs, containsInAnyOrder(
                new PluggableArtifactConfig("s3", "cd.go.s3"),
                new PluggableArtifactConfig("docker", "cd.go.docker")
        ));
    }

    @Test
    public void findByArtifactId_shouldReturnPluggableArtifactConfigs() {
        ArtifactTypeConfigs allConfigs = new ArtifactTypeConfigs();
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final PluggableArtifactConfig s3 = allConfigs.findByArtifactId("s3");
        assertThat(s3, is(new PluggableArtifactConfig("s3", "cd.go.s3")));
    }

    @Test
    public void findByArtifactId_shouldReturnNullWhenPluggableArtifactConfigNotExistWithGivenId() {
        ArtifactTypeConfigs allConfigs = new ArtifactTypeConfigs();
        allConfigs.add(new PluggableArtifactConfig("s3", "cd.go.s3"));
        allConfigs.add(new PluggableArtifactConfig("docker", "cd.go.docker"));

        final PluggableArtifactConfig s3 = allConfigs.findByArtifactId("foo");
        assertNull(s3);
    }
}
