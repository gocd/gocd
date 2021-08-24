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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static com.thoughtworks.go.serverhealth.ServerHealthMatcher.containsState;
import static com.thoughtworks.go.serverhealth.ServerHealthMatcher.doesNotContainState;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ArtifactsDirChangeTest {

    @Autowired private GoConfigService goConfigService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao configDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ArtifactsDirHolder artifactsDirHolder;
    @Autowired private TransactionTemplate transactionTemplate;

    private GoConfigFileHelper configHelper ;
    private PipelineWithTwoStages fixture;


    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(configDao);
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        serverHealthService.removeAllLogs();
    }

    @AfterEach
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        fixture.onTearDown();
    }

    @Test
    public void shouldLogErrorWhenArtifactsDirChanged() {
        changeArtifactsDirAndThenTryToUseIt("/tmp/invalid-dir");
        assertThat(serverHealthService, containsState(ArtifactsDirHolder.ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE, HealthStateLevel.WARNING, ArtifactsDirHolder.ARTIFACTS_ROOT_CHANGED_MESSAGE));
    }

    @Test
    public void shouldRemoveLogAfterArtifactsDirIsRecovered() {
        changeArtifactsDirAndThenTryToUseIt("/tmp/invalid-dir");
        assertThat(serverHealthService, containsState(ArtifactsDirHolder.ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE));
        changeArtifactsDirAndThenTryToUseIt(artifactsDirHolder.getArtifactsDir().getPath());
        assertThat(serverHealthService, doesNotContainState(ArtifactsDirHolder.ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE));
    }

    private void changeArtifactsDirAndThenTryToUseIt(String dir) {
        configHelper.setArtifactsDir(dir);
        goConfigService.getCurrentConfig();
    }
}
