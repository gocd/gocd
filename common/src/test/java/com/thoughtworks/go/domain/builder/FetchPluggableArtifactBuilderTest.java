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
package com.thoughtworks.go.domain.builder;

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.FetchPluggableArtifactTask;
import com.thoughtworks.go.domain.ChecksumFileHandler;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static com.thoughtworks.go.remote.work.artifact.ArtifactsPublisher.PLUGGABLE_ARTIFACT_METADATA_FOLDER;
import static com.thoughtworks.go.util.command.TaggedStreamConsumer.OUT;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class FetchPluggableArtifactBuilderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File metadataDest;
    private String sourceOnServer;
    private JobIdentifier jobIdentifier;
    private ArtifactStore artifactStore;
    private DefaultGoPublisher publisher;
    private ArtifactExtension artifactExtension;
    private ChecksumFileHandler checksumFileHandler;
    private FetchPluggableArtifactTask fetchPluggableArtifactTask;
    private PluginRequestProcessorRegistry registry;
    private static final String PLUGIN_ID = "cd.go.s3";

    @Before
    public void setUp() throws Exception {
        publisher = mock(DefaultGoPublisher.class);
        artifactExtension = mock(ArtifactExtension.class);
        checksumFileHandler = mock(ChecksumFileHandler.class);
        registry = mock(PluginRequestProcessorRegistry.class);

        metadataDest = new File(temporaryFolder.newFolder("dest"), "cd.go.s3.json");
        FileUtils.writeStringToFile(metadataDest, "{\"artifactId\":{}}", StandardCharsets.UTF_8);

        jobIdentifier = new JobIdentifier("cruise", -10, "1", "dev", "1", "windows", 1L);
        artifactStore = new ArtifactStore("s3", PLUGIN_ID, ConfigurationPropertyMother.create("ACCESS_KEY", true, "hksjdfhsksdfh"));

        fetchPluggableArtifactTask = new FetchPluggableArtifactTask(new CaseInsensitiveString("dev"),
                new CaseInsensitiveString("windows"),
                "artifactId",
                ConfigurationPropertyMother.create("Destination", false, "build/"));

        sourceOnServer = format("%s/%s", PLUGGABLE_ARTIFACT_METADATA_FOLDER, "cd.go.s3.json");

        when(checksumFileHandler.handleResult(SC_OK, publisher)).thenReturn(true);
    }

    @Test
    public void shouldCallPublisherToFetchMetadataFile() {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);

        builder.build(publisher, new EnvironmentVariableContext(), null, artifactExtension, registry, "utf-8");

        final ArgumentCaptor<FetchArtifactBuilder> argumentCaptor = ArgumentCaptor.forClass(FetchArtifactBuilder.class);

        verify(publisher).fetch(argumentCaptor.capture());

        final FetchArtifactBuilder fetchArtifactBuilder = argumentCaptor.getValue();

        assertThat(fetchArtifactBuilder.getSrc(), is("pluggable-artifact-metadata/cd.go.s3.json"));
        assertThat(fetchArtifactBuilder.getDest(), is("cd.go.s3.json"));
    }

    @Test
    public void shouldCallArtifactExtension() {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);

        builder.build(publisher, new EnvironmentVariableContext(), null, artifactExtension, registry, "utf-8");

        verify(artifactExtension).fetchArtifact(eq(PLUGIN_ID), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
    }

    @Test
    public void shouldCallArtifactExtensionWithMetadata() throws IOException {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);
        final Map<String, Object> metadata = Collections.singletonMap("Version", "10.12.0");

        FileUtils.writeStringToFile(metadataDest, new Gson().toJson(metadata), StandardCharsets.UTF_8);

        builder.build(publisher, new EnvironmentVariableContext(), null, artifactExtension, registry, "utf-8");

        verify(artifactExtension).fetchArtifact(eq(PLUGIN_ID), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
    }

    @Test
    public void shouldRegisterAndDeRegisterArtifactRequestProcessBeforeAndAfterPublishingPluggableArtifact() throws IOException {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);
        final Map<String, Object> metadata = Collections.singletonMap("Version", "10.12.0");

        FileUtils.writeStringToFile(metadataDest, new Gson().toJson(metadata), StandardCharsets.UTF_8);

        builder.build(publisher, new EnvironmentVariableContext(), null, artifactExtension, registry, "utf-8");

        InOrder inOrder = inOrder(registry, artifactExtension);
        inOrder.verify(registry, times(1)).registerProcessorFor(eq(CONSOLE_LOG.requestName()), ArgumentMatchers.any(ArtifactRequestProcessor.class));
        inOrder.verify(artifactExtension).fetchArtifact(eq(PLUGIN_ID), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
        inOrder.verify(registry, times(1)).removeProcessorFor(CONSOLE_LOG.requestName());
    }

    @Test
    public void shouldUpdateEnvironmentVariableContextAfterFetchingArtifact() {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("VAR1", "old-value1", false);
        environmentVariableContext.setProperty("VAR2", "old-value2", true);
        environmentVariableContext.setProperty("VAR3", "old-value3", true);
        environmentVariableContext.setProperty("VAR4", "old-value4", true);

        when(artifactExtension.fetchArtifact(eq(PLUGIN_ID), eq(artifactStore), any(), anyMap(), eq(metadataDest.getParent())))
                .thenReturn(Arrays.asList(
                        new FetchArtifactEnvironmentVariable("VAR1", "value1-is-now-secure", true),
                        new FetchArtifactEnvironmentVariable("VAR2", "value2-is-now-insecure", false),
                        new FetchArtifactEnvironmentVariable("VAR3", "value3-but-secure-is-unchanged", true),
                        new FetchArtifactEnvironmentVariable("VAR5", "new-value5-insecure", false),
                        new FetchArtifactEnvironmentVariable("VAR6", "new-value6-secure", true)
                ));

        builder.build(publisher, environmentVariableContext, null, artifactExtension, registry, "utf-8");

        Map<String, String> newVariablesAfterFetchArtifact = environmentVariableContext.getProperties();

        assertThat(newVariablesAfterFetchArtifact.size(), is(6));

        assertVariable(environmentVariableContext, "VAR1", "value1-is-now-secure", true);
        assertVariable(environmentVariableContext, "VAR2", "value2-is-now-insecure", false);
        assertVariable(environmentVariableContext, "VAR3", "value3-but-secure-is-unchanged", true);
        assertVariable(environmentVariableContext, "VAR4", "old-value4", true);
        assertVariable(environmentVariableContext, "VAR5", "new-value5-insecure", false);
        assertVariable(environmentVariableContext, "VAR6", "new-value6-secure", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(publisher, atLeastOnce()).taggedConsumeLine(eq(OUT), captor.capture());


        assertThat(captor.getAllValues(), hasItems(
                "WARNING: Replacing environment variable: VAR1 = ******** (previously: old-value1)",
                "WARNING: Replacing environment variable: VAR2 = value2-is-now-insecure (previously: ********)",
                "WARNING: Replacing environment variable: VAR3 = ******** (previously: ********)",
                " NOTE: Setting new environment variable: VAR5 = new-value5-insecure",
                " NOTE: Setting new environment variable: VAR6 = ********"));

        String consoleOutput = String.join(" -- ", captor.getAllValues());
        assertThat(consoleOutput, not(containsString("value1-is-now-secure")));
        assertThat(consoleOutput, not(containsString("value3-but-secure-is-unchanged")));
        assertThat(consoleOutput, not(containsString("new-value6-secure")));
    }

    private void assertVariable(EnvironmentVariableContext environmentVariableContext, String key, String expectedValue, boolean expectedIsSecure) {
        assertThat(environmentVariableContext.getProperty(key), is(expectedValue));
        assertThat(environmentVariableContext.isPropertySecure(key), is(expectedIsSecure));
    }
}
