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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeleteArtifactStoreConfigCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private BasicCruiseConfig cruiseConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldDeleteAArtifactStore() throws Exception {
        ArtifactStore artifactStore = new ArtifactStore("foo", "cd.go.docker");
        cruiseConfig.getArtifactStores().add(artifactStore);

        new DeleteArtifactStoreConfigCommand(null, artifactStore, null, null, null).update(cruiseConfig);

        assertThat(cruiseConfig.getArtifactStores()).isEmpty();
    }

    @Test
    public void shouldRaiseExceptionInCaseArtifactStoreDoesNotExist() throws Exception {
        ArtifactStore artifactStore = new ArtifactStore("foo", "cd.go.docker");
        assertThat(cruiseConfig.getArtifactStores()).isEmpty();

        thrown.expect(RecordNotFoundException.class);

        new DeleteArtifactStoreConfigCommand(null, artifactStore, null, null, new HttpLocalizedOperationResult()).update(cruiseConfig);

        assertThat(cruiseConfig.getArtifactStores()).isEmpty();
    }

    @Test
    public void shouldNotValidateIfArtifactStoreIsUsedInPipelineConfig() {
        ArtifactStore artifactStore = new ArtifactStore("foo", "cd.go.docker");
        final PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStage("up42", "up42_stage");
        pipelineConfig.getStage("up42_stage").jobConfigByConfigName("dev").artifactTypeConfigs().add(new PluggableArtifactConfig("installers", "foo"));
        cruiseConfig.addPipeline("Foo", pipelineConfig);

        DeleteArtifactStoreConfigCommand command = new DeleteArtifactStoreConfigCommand(null, artifactStore, null, null, new HttpLocalizedOperationResult());

        thrown.expect(GoConfigInvalidException.class);
        thrown.expectMessage("The artifact store 'foo' is being referenced by pipeline(s): JobConfigIdentifier[up42:up42_stage:dev].");

        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldValidateIfArtifactStoreIsNotInUseByPipeline() {
        ArtifactStore artifactStore = new ArtifactStore("foo", "cd.go.docker");
        assertThat(cruiseConfig.getArtifactStores()).isEmpty();

        DeleteArtifactStoreConfigCommand command = new DeleteArtifactStoreConfigCommand(null, artifactStore, null, null, new HttpLocalizedOperationResult());

        assertTrue(command.isValid(cruiseConfig));
    }
}
