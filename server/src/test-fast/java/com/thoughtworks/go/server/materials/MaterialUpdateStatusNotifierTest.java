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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.materials.Material;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class MaterialUpdateStatusNotifierTest {
    private MaterialUpdateCompletedTopic mockTopic;
    private MaterialUpdateStatusNotifier materialUpdateStatusNotifier;

    @Before public void setUp() throws Exception {
        mockTopic = Mockito.mock(MaterialUpdateCompletedTopic.class);
        materialUpdateStatusNotifier = new MaterialUpdateStatusNotifier(mockTopic);
    }

    @Test public void shouldAddItselfToATheUpdateCompletedTopicOnConstruction() throws Exception {

        MaterialUpdateStatusNotifier updateStatusNotifier = new MaterialUpdateStatusNotifier(mockTopic);

        Mockito.verify(mockTopic).addListener(updateStatusNotifier);
    }

    @Test public void shouldKnowAboutAListenerBasedOnAPipelineConfig() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("config"), new MaterialConfigs());
        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig, Mockito.mock(MaterialUpdateStatusListener.class));
        assertThat(materialUpdateStatusNotifier.hasListenerFor(pipelineConfig), is(true));
        materialUpdateStatusNotifier.removeListenerFor(pipelineConfig);
        assertThat(materialUpdateStatusNotifier.hasListenerFor(pipelineConfig), is(false));
    }

    @Test public void shouldNotifyListenerWhenItsMaterialIsUpdated() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("config"), new MaterialConfigs());
        Material material = new HgMaterial("url", null);
        pipelineConfig.addMaterialConfig(material.config());
        MaterialUpdateStatusListener mockStatusListener = Mockito.mock(MaterialUpdateStatusListener.class);
        when(mockStatusListener.isListeningFor(material)).thenReturn(true);

        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig, mockStatusListener);
        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(material, 123));
        Mockito.verify(mockStatusListener).onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material, 123));
    }

    @Test public void shouldNotNotifyListenerWhenUnknownMaterialIsUpdated() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("config"), new MaterialConfigs());
        MaterialUpdateStatusListener mockStatusListener = Mockito.mock(MaterialUpdateStatusListener.class);

        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig, mockStatusListener);

        HgMaterial material = new HgMaterial("url", null);
        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(material, 1234));
        Mockito.verify(mockStatusListener, Mockito.never()).onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material, 1234));
    }

    @Test public void shouldBeAbleToUnregisterAListenerDuringACallback() throws Exception {
        final PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("config"), new MaterialConfigs());
        Material material = new HgMaterial("url", null);
        pipelineConfig.addMaterialConfig(material.config());
        MaterialUpdateStatusListener statusListener = new MaterialUpdateStatusListener() {
            @Override
            public void onMaterialUpdate(MaterialUpdateCompletedMessage message) {
                materialUpdateStatusNotifier.removeListenerFor(pipelineConfig);
            }

            @Override
            public boolean isListeningFor(Material material) {
                return true;
            }
        };

        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig, statusListener);
        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(material, 123));

        assertThat(materialUpdateStatusNotifier.hasListenerFor(pipelineConfig), is(false));
    }

    @Test public void shouldNotifyListenerWhenItsMaterialIsUpdatedEvenIfAnotherListenerThrowsAnException() throws Exception {
        Material sharedMaterial = new HgMaterial("url", null);

        PipelineConfig pipelineConfig1 = new PipelineConfig(new CaseInsensitiveString("config"), new MaterialConfigs());

        pipelineConfig1.addMaterialConfig(sharedMaterial.config());
        PipelineConfig pipelineConfig2 = new PipelineConfig(new CaseInsensitiveString("another-config"), new MaterialConfigs());

        pipelineConfig2.addMaterialConfig(sharedMaterial.config());

        MaterialUpdateStatusListener badListener = Mockito.mock(MaterialUpdateStatusListener.class);
        Mockito.doThrow(new RuntimeException("foo")).when(badListener).onMaterialUpdate(new MaterialUpdateSuccessfulMessage(sharedMaterial, 123));

        MaterialUpdateStatusListener goodListener = Mockito.mock(MaterialUpdateStatusListener.class);

        when(badListener.isListeningFor(sharedMaterial)).thenReturn(true);
        when(goodListener.isListeningFor(sharedMaterial)).thenReturn(true);
        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig1, badListener);
        materialUpdateStatusNotifier.registerListenerFor(pipelineConfig2, goodListener);

        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(sharedMaterial, 123));

        Mockito.verify(badListener).onMaterialUpdate(new MaterialUpdateSuccessfulMessage(sharedMaterial, 123));
        Mockito.verify(goodListener).onMaterialUpdate(new MaterialUpdateSuccessfulMessage(sharedMaterial, 123));
    }
}
