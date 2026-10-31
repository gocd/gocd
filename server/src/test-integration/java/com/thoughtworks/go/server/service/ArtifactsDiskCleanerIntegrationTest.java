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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.util.FileSizeUtils.fromGigaToBytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class ArtifactsDiskCleanerIntegrationTest {
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private ArtifactsService artifactsService;
    @Autowired private StageService stageService;
    @Autowired private ConfigDbStateRepository configDbStateRepository;
    @Autowired private DatabaseAccessHelper dbHelper;

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private final List<File> pipelineDirectories = new ArrayList<>();
    private SystemDiskSpaceChecker diskSpaceChecker;
    private ArtifactsDiskCleaner artifactsDiskCleaner;

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        diskSpaceChecker = mock(SystemDiskSpaceChecker.class);
        artifactsDiskCleaner = new ArtifactsDiskCleaner(systemEnvironment, goConfigService, diskSpaceChecker, artifactsService, stageService, configDbStateRepository);
    }

    @AfterEach
    public void tearDown() throws Exception {
        pipelineDirectories.forEach(FileUtils::deleteQuietly);
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldOnlyPurgeArtifactsOfOldestStages_unlessPurgingArtifactDirectories() throws Exception {
        Stage oldest = completedStageWithArtifacts("pipeline-1");
        Stage newest = completedStageWithArtifacts("pipeline-2");
        configure(1.0, 2.0, false);

        diskSpaceIsLowWhile(artifactOf(oldest));
        artifactsDiskCleaner.deleteOldArtifacts();

        assertThat(artifactOf(oldest)).doesNotExist();
        assertThat(consoleLogOf(oldest)).exists();
        assertThat(artifactOf(newest)).exists();
        assertThat(consoleLogOf(newest)).exists();
        assertThat(identifiersOf(stageService.oldestStagesWithDeletableArtifacts())).containsExactly(newest.getIdentifier());
        assertThat(identifiersOf(stageService.oldestStagesWithPurgedArtifacts(0))).containsExactly(oldest.getIdentifier());
    }

    @Test
    public void shouldPurgeArtifactDirectoriesOfOldestStages_includingThoseThatHadTheirArtifactsPurgedEarlier() throws Exception {
        Stage neverCleanedUp = completedStageWithArtifacts("pipeline-0");
        Stage oldest = completedStageWithArtifacts("pipeline-1");
        Stage older = completedStageWithArtifacts("pipeline-2");
        Stage newer = completedStageWithArtifacts("pipeline-3");
        Stage newest = completedStageWithArtifacts("pipeline-4");
        prohibitArtifactCleanupOf("pipeline-0");

        configure(1.0, 2.0, false);
        diskSpaceIsLowWhile(artifactOf(oldest));
        artifactsDiskCleaner.deleteOldArtifacts();

        assertThat(artifactOf(oldest)).doesNotExist();
        assertThat(consoleLogOf(oldest)).exists();

        configure(1.0, 2.0, true);
        diskSpaceIsLowWhile(consoleLogOf(older));
        artifactsDiskCleaner.deleteOldArtifacts();

        assertThat(pipelineDirectoryOf(oldest)).isEmptyDirectory();
        assertThat(pipelineDirectoryOf(older)).isEmptyDirectory();
        assertThat(artifactOf(newer)).exists();
        assertThat(consoleLogOf(newer)).exists();
        assertThat(artifactOf(newest)).exists();
        assertThat(consoleLogOf(newest)).exists();
        assertThat(artifactOf(neverCleanedUp)).exists();
        assertThat(consoleLogOf(neverCleanedUp)).exists();
        assertThat(identifiersOf(stageService.oldestStagesWithDeletableArtifacts())).containsExactly(newer.getIdentifier(), newest.getIdentifier());
        assertThat(identifiersOf(stageService.oldestStagesWithPurgedArtifacts(0))).containsExactly(oldest.getIdentifier(), older.getIdentifier());

        diskSpaceIsLowWhile(consoleLogOf(newer));
        artifactsDiskCleaner.deleteOldArtifacts();

        assertThat(pipelineDirectoryOf(newer)).isEmptyDirectory();
        assertThat(artifactOf(newest)).exists();
        assertThat(consoleLogOf(newest)).exists();
        assertThat(artifactOf(neverCleanedUp)).exists();
        assertThat(consoleLogOf(neverCleanedUp)).exists();
    }

    private Stage completedStageWithArtifacts(String pipelineName) throws Exception {
        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, "stage", "job");
        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(pipeline);
        Stage stage = pipeline.getFirstStage();
        pipelineDirectories.add(pipelineDirectoryOf(stage));
        FileUtils.deleteQuietly(pipelineDirectoryOf(stage));
        FileUtils.forceMkdirParent(consoleLogOf(stage));
        Files.writeString(artifactOf(stage).toPath(), "some artifact", UTF_8);
        Files.writeString(consoleLogOf(stage).toPath(), "some console log", UTF_8);
        return stage;
    }

    private File artifactOf(Stage stage) throws Exception {
        return artifactsService.findArtifact(new JobIdentifier(stage.getIdentifier(), "job"), "artifact.txt");
    }

    private File consoleLogOf(Stage stage) throws Exception {
        return artifactsService.findArtifact(new JobIdentifier(stage.getIdentifier(), "job"), "cruise-output/console.log");
    }

    private File pipelineDirectoryOf(Stage stage) throws Exception {
        File jobDirectory = artifactsService.findArtifact(new JobIdentifier(stage.getIdentifier(), "job"), "");
        File pipelineInstanceDirectory = jobDirectory.getParentFile().getParentFile().getParentFile();
        return pipelineInstanceDirectory.getParentFile();
    }

    private void configure(Double purgeStart, Double purgeUpto, boolean purgeArtifactDirectories) {
        CruiseConfig cruiseConfig = configHelper.currentConfig();
        cruiseConfig.server().setPurgeLimits(purgeStart, purgeUpto);
        cruiseConfig.server().getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(purgeArtifactDirectories);
        configHelper.writeConfigFile(cruiseConfig);
    }

    private void prohibitArtifactCleanupOf(String pipelineName) {
        CruiseConfig cruiseConfig = configHelper.currentConfig();
        ReflectionUtil.setField(cruiseConfig.pipelineConfigByName(cis(pipelineName)).getFirst(), "artifactCleanupProhibited", true);
        configHelper.writeConfigFile(cruiseConfig);
    }

    private void diskSpaceIsLowWhile(File file) {
        when(diskSpaceChecker.getUsableSpaceBytes(any())).thenAnswer(invocation -> file.exists() ? 0L : fromGigaToBytes(10));
    }

    private static List<Object> identifiersOf(List<Stage> stages) {
        return new ArrayList<>(stages.stream().map(Stage::getIdentifier).toList());
    }
}
