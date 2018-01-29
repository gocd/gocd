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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.update.ArtifactStoreCreateCommand;
import com.thoughtworks.go.config.update.ArtifactStoreDeleteCommand;
import com.thoughtworks.go.config.update.ArtifactStoreUpdateCommand;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

        verify(configService).updateConfig(any(ArtifactStoreCreateCommand.class), eq(admin));
    }

    @Test
    public void update_shouldUpdateAArtifactStoreToConfig() {
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.docker");
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        artifactStoreService.update(admin, "md5", artifactStore, result);

        verify(configService).updateConfig(any(ArtifactStoreUpdateCommand.class), eq(admin));
    }

    @Test
    public void delete_shouldDeleteAArtifactStoreToConfig() {
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.docker");
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        artifactStoreService.delete(admin, artifactStore, result);

        verify(configService).updateConfig(any(ArtifactStoreDeleteCommand.class), eq(admin));
    }
}
