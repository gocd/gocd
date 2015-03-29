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
package com.thoughtworks.go.plugin.access.artifactcleanup;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactCleanupExtensionTest {

    private PluginManager pluginManager;
    private ArtifactCleanupExtension artifactCleanupExtension;
    private static final String PLUGIN_ID = "plugin-id";
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;


    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        artifactCleanupExtension = new ArtifactCleanupExtension(pluginManager);
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, asList("1.0"))).thenReturn("1.0");
    }

    @Test
    public void shouldTalkToPluginToGetStageInstancesForArtifactCleanup() throws Exception {
        ArtifactExtensionStageConfiguration stage = new ArtifactExtensionStageConfiguration("p", "s1");
        ArtifactExtensionStageConfiguration anotherStage = new ArtifactExtensionStageConfiguration("p", "s2");

        String expectedResponseBody = "[{\"id\":\"1\",\"pipeline\":\"p\",\"stage\":\"s1\",\"pipeline-counter\":\"1\",\"stage-counter\":\"1\"}," +
                "{\"id\":\"2\",\"pipeline\":\"p\",\"stage\":\"s2\",\"pipeline-counter\":\"1\",\"stage-counter\":\"1\"}]";

        when(pluginManager.isPluginOfType(ArtifactCleanupExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        List<ArtifactExtensionStageInstance> stageInstances = artifactCleanupExtension.getStageInstancesForArtifactCleanup(PLUGIN_ID, asList(stage, anotherStage));

        assertArtifactExtensionStageInstance(stageInstances.get(0), 1L, "p", 1, "s1", "1");
        assertArtifactExtensionStageInstance(stageInstances.get(1), 2L, "p", 1, "s2", "1");

    }

    private void assertArtifactExtensionStageInstance(ArtifactExtensionStageInstance artifactExtensionStageInstance, long id, String pipeline, int pipelineCounter, String stage, String stageCounter) {
        assertThat(artifactExtensionStageInstance.getId(), is(id));
        assertThat(artifactExtensionStageInstance.getPipelineName(), is(pipeline));
        assertThat(artifactExtensionStageInstance.getPipelineCounter(), is(pipelineCounter));
        assertThat(artifactExtensionStageInstance.getStageName(), is(stage));
        assertThat(artifactExtensionStageInstance.getStageCounter(), is(stageCounter));
    }
}