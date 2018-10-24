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

package com.thoughtworks.go.plugin.access.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.FetchPluggableArtifactTask;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.*;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ARTIFACT_EXTENSION;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

class ArtifactExtensionForV1Test extends ArtifactExtensionTestBase {
    @Override
    String versionToTestAgainst() {
        return ArtifactMessageConverterV1.VERSION;
    }

    @Test
    void shouldSubmitFetchArtifactRequest() {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ARTIFACT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(""));
        final FetchPluggableArtifactTask pluggableArtifactTask = new FetchPluggableArtifactTask(null, null, "artifactId", create("Filename", false, "build/libs/foo.jar"));

        List<FetchArtifactEnvironmentVariable> environmentVariables = artifactExtension.fetchArtifact(PLUGIN_ID, new ArtifactStore("s3", "cd.go.s3"), pluggableArtifactTask.getConfiguration(), Collections.singletonMap("Version", "10.12.0"), "/temp");

        final GoPluginApiRequest request = requestArgumentCaptor.getValue();

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

        assertThat(request.extension(), is(ARTIFACT_EXTENSION));
        assertThat(request.requestName(), is(REQUEST_FETCH_ARTIFACT));
        assertThatJson(request.requestBody()).isEqualTo(requestHash);

        assertThat(environmentVariables, is([]))
    }
}
