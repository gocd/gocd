/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.artifact;

import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.REQUEST_PUBLISH_ARTIFACT;
import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.REQUEST_PUBLISH_ARTIFACT_METADATA;
import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.REQUEST_STORE_CONFIG_METADATA;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ARTIFACT_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ArtifactExtensionTest {

    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldGetSupportedVersions() {
        final ArtifactExtension artifactExtension = new ArtifactExtension(null);

        assertThat(artifactExtension.goSupportedVersions(), containsInAnyOrder("1.0"));
    }

    @Test
    public void shouldRegisterMessageHandler() {
        final ArtifactExtension artifactExtension = new ArtifactExtension(null);

        assertTrue(artifactExtension.getMessageHandler(ArtifactMessageConverterV1.VERSION) instanceof ArtifactMessageConverterV1);
    }

    @Test
    public void shouldSubmitPublishArtifactRequest() {
        final ArtifactExtension artifactExtension = new ArtifactExtension(pluginManager);
        final ArgumentCaptor<GoPluginApiRequest> captor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.isPluginOfType(ARTIFACT_EXTENSION, "foo.plugin")).thenReturn(true);
        when(pluginManager.resolveExtensionVersion("foo.plugin", artifactExtension.goSupportedVersions())).thenReturn("1.0");
        when(pluginManager.submitTo(eq("foo.plugin"), captor.capture())).thenReturn(DefaultGoPluginApiResponse.success("{\"artifact-version\":\"10.12.0\"}"));

        final Map<String, Object> response = artifactExtension.publishArtifact("foo.plugin", new ArtifactStores(), new ArrayList<>());

        final GoPluginApiRequest request = captor.getValue();

        assertThat(request.extension(), is(ARTIFACT_EXTENSION));
        assertThat(request.requestName(), is(REQUEST_PUBLISH_ARTIFACT));
        assertThat(request.requestBody(), is("{\"artifactPlans\":[],\"artifactStores\":[]}"));

        assertThat(response.size(), is(1));
        assertThat(response, hasEntry("artifact-version", "10.12.0"));
    }

    @Test
    public void shouldGetArtifactStoreMetadataFromPlugin() {
        String responseBody = "[{\"key\":\"BUCKET_NAME\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"AWS_ACCESS_KEY\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        final ArtifactExtension artifactExtension = new ArtifactExtension(pluginManager);
        final ArgumentCaptor<GoPluginApiRequest> captor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.isPluginOfType(ARTIFACT_EXTENSION, "foo.plugin")).thenReturn(true);
        when(pluginManager.resolveExtensionVersion("foo.plugin", artifactExtension.goSupportedVersions())).thenReturn("1.0");
        when(pluginManager.submitTo(eq("foo.plugin"), captor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final List<PluginConfiguration> response = artifactExtension.getArtifactStoreMetadata("foo.plugin");

        final GoPluginApiRequest request = captor.getValue();

        assertThat(request.extension(), is(ARTIFACT_EXTENSION));
        assertThat(request.requestName(), is(REQUEST_STORE_CONFIG_METADATA));
        assertNull(request.requestBody());

        assertThat(response.size(), is(2));
        assertThat(response, containsInAnyOrder(
                new PluginConfiguration("BUCKET_NAME", new Metadata(true, false)),
                new PluginConfiguration("AWS_ACCESS_KEY", new Metadata(true, true))
        ));
    }

    @Test
    public void shouldGetPublishArtifactMetadataFromPlugin() {
        String responseBody = "[{\"key\":\"FILENAME\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"VERSION\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        final ArtifactExtension artifactExtension = new ArtifactExtension(pluginManager);
        final ArgumentCaptor<GoPluginApiRequest> captor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.isPluginOfType(ARTIFACT_EXTENSION, "foo.plugin")).thenReturn(true);
        when(pluginManager.resolveExtensionVersion("foo.plugin", artifactExtension.goSupportedVersions())).thenReturn("1.0");
        when(pluginManager.submitTo(eq("foo.plugin"), captor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final List<PluginConfiguration> response = artifactExtension.getPublishArtifactMetadata("foo.plugin");

        final GoPluginApiRequest request = captor.getValue();

        assertThat(request.extension(), is(ARTIFACT_EXTENSION));
        assertThat(request.requestName(), is(REQUEST_PUBLISH_ARTIFACT_METADATA));
        assertNull(request.requestBody());

        assertThat(response.size(), is(2));
        assertThat(response, containsInAnyOrder(
                new PluginConfiguration("FILENAME", new Metadata(true, false)),
                new PluginConfiguration("VERSION", new Metadata(true, true))
        ));
    }
}