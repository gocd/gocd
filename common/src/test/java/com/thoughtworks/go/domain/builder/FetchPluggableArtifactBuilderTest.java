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
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static com.thoughtworks.go.remote.work.artifact.ArtifactsPublisher.PLUGGABLE_ARTIFACT_METADATA_FOLDER;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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

    @Before
    public void setUp() throws Exception {
        publisher = mock(DefaultGoPublisher.class);
        artifactExtension = mock(ArtifactExtension.class);
        checksumFileHandler = mock(ChecksumFileHandler.class);
        registry = mock(PluginRequestProcessorRegistry.class);

        metadataDest = new File(temporaryFolder.newFolder("dest"), "cd.go.s3.json");
        FileUtils.writeStringToFile(metadataDest, "{\"artifactId\":{}}", StandardCharsets.UTF_8);

        jobIdentifier = new JobIdentifier("cruise", -10, "1", "dev", "1", "windows", 1L);
        artifactStore = new ArtifactStore("s3", "cd.go.s3", ConfigurationPropertyMother.create("ACCESS_KEY", true, "hksjdfhsksdfh"));

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

        builder.build(publisher, null, null, artifactExtension, registry, "utf-8");

        final ArgumentCaptor<FetchArtifactBuilder> argumentCaptor = ArgumentCaptor.forClass(FetchArtifactBuilder.class);

        verify(publisher).fetch(argumentCaptor.capture());

        final FetchArtifactBuilder fetchArtifactBuilder = argumentCaptor.getValue();

        assertThat(fetchArtifactBuilder.getSrc(), is("pluggable-artifact-metadata/cd.go.s3.json"));
        assertThat(fetchArtifactBuilder.getDest(), is("cd.go.s3.json"));
    }

    @Test
    public void shouldCallArtifactExtension() {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);

        builder.build(publisher, null, null, artifactExtension, registry, "utf-8");

        verify(artifactExtension).fetchArtifact(eq("cd.go.s3"), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
    }

    @Test
    public void shouldCallArtifactExtensionWithMetadata() throws IOException {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);
        final Map<String, Object> metadata = Collections.singletonMap("Version", "10.12.0");

        final FileWriter fileWriter = new FileWriter(metadataDest);
        fileWriter.write(new Gson().toJson(metadata));
        fileWriter.close();

        builder.build(publisher, null, null, artifactExtension, registry, "utf-8");

        verify(artifactExtension).fetchArtifact(eq("cd.go.s3"), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
    }

    @Test
    public void shouldRegisterAndDeRegisterArtifactRequestProcessBeforeAndAfterPublishingPluggableArtifact() throws IOException {
        final FetchPluggableArtifactBuilder builder = new FetchPluggableArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobIdentifier, artifactStore, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask.getArtifactId(), sourceOnServer, metadataDest, checksumFileHandler);
        final Map<String, Object> metadata = Collections.singletonMap("Version", "10.12.0");

        final FileWriter fileWriter = new FileWriter(metadataDest);
        fileWriter.write(new Gson().toJson(metadata));
        fileWriter.close();

        builder.build(publisher, null, null, artifactExtension, registry, "utf-8");


        InOrder inOrder = inOrder(registry, artifactExtension);
        inOrder.verify(registry, times(1)).registerProcessorFor(eq(CONSOLE_LOG.requestName()), any(ArtifactRequestProcessor.class));
        inOrder.verify(artifactExtension).fetchArtifact(eq("cd.go.s3"), eq(artifactStore), eq(fetchPluggableArtifactTask.getConfiguration()), any(), eq(metadataDest.getParent()));
        inOrder.verify(registry, times(1)).removeProcessorFor(CONSOLE_LOG.requestName());
    }
}