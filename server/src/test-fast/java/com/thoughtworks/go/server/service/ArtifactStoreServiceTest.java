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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.update.CreateArtifactStoreConfigCommand;
import com.thoughtworks.go.config.update.DeleteArtifactStoreConfigCommand;
import com.thoughtworks.go.config.update.UpdateArtifactStoreConfigCommand;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ArtifactStoreServiceTest {
    @Mock
    private GoConfigService configService;
    @Mock
    private ArtifactExtension extension;
    @Mock
    private EntityHashingService entityHashingService;
    private ArtifactStoreService artifactStoreService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        artifactStoreService = new ArtifactStoreService(configService, entityHashingService, extension);
    }

    @Test
    public void findArtifactStore_shouldFindArtifactStoreByStoreId() {
        when(configService.artifactStores()).thenReturn(new ArtifactStores(
                new ArtifactStore("docker", "cd.go.docker"),
                new ArtifactStore("s3", "cd.go.s3")
        ));


        final ArtifactStore dockerArtifactStore = artifactStoreService.findArtifactStore("docker");
        assertThat(dockerArtifactStore).isEqualTo(new ArtifactStore("docker", "cd.go.docker"));

        final ArtifactStore s3ArtifactStore = artifactStoreService.findArtifactStore("s3");
        assertThat(s3ArtifactStore).isEqualTo(new ArtifactStore("s3", "cd.go.s3"));
    }

    @Test
    public void getPluginProfiles_shouldGetAllArtifactStores() {
        when(configService.artifactStores()).thenReturn(new ArtifactStores(
                new ArtifactStore("docker", "cd.go.docker"),
                new ArtifactStore("s3", "cd.go.s3")
        ));

        final ArtifactStores artifactStores = artifactStoreService.getPluginProfiles();

        assertThat(artifactStores).hasSize(2).contains(
                new ArtifactStore("docker", "cd.go.docker"),
                new ArtifactStore("s3", "cd.go.s3"));
    }

    @Test
    public void create_shouldAddAArtifactStoreToConfig() {
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.docker");
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        artifactStoreService.create(admin, artifactStore, result);

        verify(configService).updateConfig(any(CreateArtifactStoreConfigCommand.class), eq(admin));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeAddingTheArtifactStore() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker", create("key", false, "val"));
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "some-error"));


        when(extension.validateArtifactStoreConfig(eq("cd.go.artifact.docker"), anyMap())).thenReturn(validationResult);

        artifactStoreService.create(new Username("admin"), artifactStore, new HttpLocalizedOperationResult());

        MatcherAssert.assertThat(artifactStore.first().errors().size(), is(1));
        MatcherAssert.assertThat(artifactStore.first().errors().on("key"), is("some-error"));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeUpdatingTheArtifactStore() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker", create("key", false, "val"));
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "some-error"));


        when(extension.validateArtifactStoreConfig(eq("cd.go.artifact.docker"), anyMap())).thenReturn(validationResult);

        artifactStoreService.update(new Username("admin"), "md5", artifactStore, new HttpLocalizedOperationResult());

        MatcherAssert.assertThat(artifactStore.first().errors().size(), is(1));
        MatcherAssert.assertThat(artifactStore.first().errors().on("key"), is("some-error"));
    }

    @Test
    public void shouldNotPerformPluginValidationsWhenDeletingElasticProfile() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker", create("key", false, "val"));

        Username username = new Username("username");
        artifactStoreService.delete(username, artifactStore, new HttpLocalizedOperationResult());

        verify(extension, never()).validateArtifactStoreConfig(artifactStore.getPluginId(), artifactStore.getConfigurationAsMap(true));
    }

    @Test
    public void update_shouldUpdateAArtifactStoreToConfig() {
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.docker");
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        artifactStoreService.update(admin, "md5", artifactStore, result);

        verify(configService).updateConfig(any(UpdateArtifactStoreConfigCommand.class), eq(admin));
    }

    @Test
    public void delete_shouldDeleteAArtifactStoreToConfig() {
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.docker");
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        artifactStoreService.delete(admin, artifactStore, result);

        verify(configService).updateConfig(any(DeleteArtifactStoreConfigCommand.class), eq(admin));
    }
}
