/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class MaterialDatabaseDependencyUpdaterIntegrationTest  {

    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigService goConfigService;
    @Autowired private MaterialDatabaseUpdater updater;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();
        configHelper.onSetUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldInsertAllRunsAfterLastKnownOfUpstreamStage() throws Exception {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("acceptance", "stage-name", "job");
        goConfigService.addPipeline(mingleConfig, "pipeline-group");

        Pipeline passed1 = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.passStage(passed1.getStages().get(0));

        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("acceptance"), new CaseInsensitiveString("stage-name"));
        String revision1 = String.format("acceptance/%s/stage-name/1", passed1.getCounter());

        updater.updateMaterial(dependencyMaterial);
        assertThat(materialRepository.findModificationWithRevision(dependencyMaterial, revision1)).isNotNull();

        Pipeline cancelledPipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.cancelStage(cancelledPipeline.getStages().get(0));

        Pipeline passed2 = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.passStage(passed2.getStages().get(0));

        updater.updateMaterial(dependencyMaterial);
        assertThat(materialRepository.findModificationWithRevision(dependencyMaterial, String.format("acceptance/%s/stage-name/1", cancelledPipeline.getCounter()))).isNull();

        Modification modification1 = materialRepository.findModificationWithRevision(dependencyMaterial, revision1);
        assertThat(modification1).isNotNull();
        assertThat(modification1.getRevision()).isEqualTo(revision1);
        assertThat(modification1.getPipelineLabel()).isEqualTo(passed1.getCounter().toString());
        assertThat(modification1.getPipelineId()).isEqualTo(passed1.getId());

        String revision2 = String.format("acceptance/%s/stage-name/1", passed2.getCounter());
        Modification modification2 = materialRepository.findModificationWithRevision(dependencyMaterial, revision2);
        assertThat(modification2).isNotNull();
        assertThat(modification2.getRevision()).isEqualTo(revision2);
        assertThat(modification2.getPipelineLabel()).isEqualTo(passed2.getCounter().toString());
        assertThat(modification2.getPipelineId()).isEqualTo(passed2.getId());
    }
}
