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
package com.thoughtworks.go.plugin.access.artifact

import com.google.gson.Gson
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.FetchPluggableArtifactTask
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse
import org.junit.Test

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.REQUEST_FETCH_ARTIFACT
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ARTIFACT_EXTENSION
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when

class ArtifactExtensionForV2Test extends ArtifactExtensionTestBase {
    @Override
    String versionToTestAgainst() {
        return ArtifactMessageConverterV2.VERSION
    }

    @Test
    void shouldSubmitFetchArtifactRequestAndParseEnvironmentVariablesReturned() {
        def requestHash = [
          artifact_metadata: [
            Version: "10.12.0"
          ],
          store_configuration: [:],
          fetch_artifact_configuration: [
            "Filename": "build/libs/foo.jar"
          ],
          "agent_working_directory": "/temp"
        ]

        def responseHash = [
          [
            name  : "VAR1",
            value : "VALUE1",
            secure: true
          ],
          [
            name  : "VAR2",
            value : "VALUE2",
            secure: false
          ]
        ]

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ARTIFACT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(new Gson().toJson(responseHash)))
        final FetchPluggableArtifactTask pluggableArtifactTask = new FetchPluggableArtifactTask(null, null, "artifactId", create("Filename", false, "build/libs/foo.jar"))

        List<FetchArtifactEnvironmentVariable> environmentVariables = artifactExtension.fetchArtifact(PLUGIN_ID, new ArtifactStore("s3", "cd.go.s3"), pluggableArtifactTask.getConfiguration(), Collections.singletonMap("Version", "10.12.0"), "/temp")

        final GoPluginApiRequest request = requestArgumentCaptor.getValue()

        assertThat(request.extension()).isEqualTo(ARTIFACT_EXTENSION)
        assertThat(request.requestName()).isEqualTo(REQUEST_FETCH_ARTIFACT)
        assertThatJson(request.requestBody()).isEqualTo(requestHash)

        assertThat(environmentVariables).isEqualTo([
          new FetchArtifactEnvironmentVariable("VAR1", "VALUE1", true),
          new FetchArtifactEnvironmentVariable("VAR2", "VALUE2", false),
        ])
    }
}
