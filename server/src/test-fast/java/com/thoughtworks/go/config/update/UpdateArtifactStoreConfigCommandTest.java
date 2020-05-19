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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateArtifactStoreConfigCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Username currentUser;
    @Mock
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        currentUser = new Username("bob");
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldRaiseErrorWhenUpdatingNonExistentArtifactStore() throws Exception {
        cruiseConfig.getArtifactStores().clear();
        final ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");

        UpdateArtifactStoreConfigCommand command = new UpdateArtifactStoreConfigCommand(null, artifactStore, null, null, new HttpLocalizedOperationResult(), null, null);

        thrown.expect(RecordNotFoundException.class);

        command.update(cruiseConfig);
    }

    @Test
    public void shouldUpdateExistingArtifactStore() throws Exception {
        final ArtifactStore oldArtifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        final ArtifactStore newArtifactStore = new ArtifactStore("docker", "cd.go.artifact.s3");

        cruiseConfig.getArtifactStores().add(oldArtifactStore);
        UpdateArtifactStoreConfigCommand command = new UpdateArtifactStoreConfigCommand(null, newArtifactStore, null, null, null, null, null);

        command.update(cruiseConfig);
        assertThat(cruiseConfig.getArtifactStores().find("docker")).isEqualTo(newArtifactStore);
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        final ArtifactStore oldArtifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        final ArtifactStore newArtifactStore = new ArtifactStore("docker", "cd.go.artifact.s3");

        cruiseConfig.getArtifactStores().add(oldArtifactStore);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.hashForEntity(oldArtifactStore)).thenReturn("digest");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdateArtifactStoreConfigCommand command = new UpdateArtifactStoreConfigCommand(goConfigService, newArtifactStore, null, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.ArtifactStore.staleConfig(newArtifactStore.getId()));
    }
}
