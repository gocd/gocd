/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.helper.GoConfigMother.createPipelineConfigWithMaterialConfig;
import static com.thoughtworks.go.helper.PipelineMother.preparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@MockitoSettings
class ScheduledPipelineLoaderTest {

    @Mock
    private PipelineSqlMapDao pipelineDao;
    @Mock
    private GoConfigService goConfigService;
    @InjectMocks
    private MaterialExpansionService materialExpansionService;

    private ScheduledPipelineLoader loader;

    @BeforeEach
    public void setUp() {
        loader = new ScheduledPipelineLoader(pipelineDao, goConfigService, null, null, null, materialExpansionService, null);
    }

    @Test
    public void materialUserPassShouldBeTakenFromConfigOnLoad() {

        // De-duplication of materials may lead to the triggering material having different credentials to the
        // configured material specific to this pipeline.
        GitMaterialConfig triggeredMaterial = MaterialConfigsMother.git("https://url", "userNameFromTrigger", null);
        GitMaterialConfig configuredMaterial = MaterialConfigsMother.git("https://url", "userFromConfig", "passFromConfig");

        assertThat(triggeredMaterial.getUserName()).isNotEqualTo(configuredMaterial.getUserName());
        assertThat(triggeredMaterial.getPassword()).isNotEqualTo(configuredMaterial.getPassword());

        when(pipelineDao.pipelineWithMaterialsAndModsByBuildId(1))
                .thenReturn(preparing(createPipelineConfigWithMaterialConfig(triggeredMaterial)));

        when(goConfigService.getCurrentConfig())
                .thenReturn(configWith(createPipelineConfigWithMaterialConfig(configuredMaterial)));

        Pipeline loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(1);

        assertThat(loadedPipeline.getMaterialRevisions())
                .singleElement()
                .satisfies(rev -> assertThat(rev.getMaterial().config())
                        .isInstanceOfSatisfying(PasswordAwareMaterial.class, mat -> {
                            assertThat(mat.getUserName()).isEqualTo("userFromConfig");
                            assertThat(mat.getPassword()).isEqualTo("passFromConfig");
                        }));
    }
}