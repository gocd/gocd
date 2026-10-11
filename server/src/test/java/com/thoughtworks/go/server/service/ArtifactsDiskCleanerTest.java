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

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.result.DiskSpaceOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.thoughtworks.go.util.FileSizeUtils.fromGigaToBytes;
import static com.thoughtworks.go.util.FileSizeUtils.fromGigaToMegabytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ArtifactsDiskCleanerTest {
    private SystemEnvironment sysEnv;
    private ArtifactsDiskCleaner artifactsDiskCleaner;
    private GoConfigService goConfigService;
    private SystemDiskSpaceChecker diskSpaceChecker;
    private ServerConfig serverConfig;
    private StageService stageService;
    private ArtifactsService artifactService;
    private ConfigDbStateRepository configDbStateRepository;

    @BeforeEach
    public void setUp() {
        sysEnv = mock(SystemEnvironment.class);

        serverConfig = new ServerConfig();
        goConfigService = mock(GoConfigService.class);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        stageService = mock(StageService.class);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);

        artifactService = mock(ArtifactsService.class);

        diskSpaceChecker = mock(SystemDiskSpaceChecker.class);

        configDbStateRepository = mock(ConfigDbStateRepository.class);

        artifactsDiskCleaner = new ArtifactsDiskCleaner(sysEnv, goConfigService, diskSpaceChecker, artifactService, stageService, configDbStateRepository);
    }

    @Test
    public void shouldTriggerOnConfiguredPurgeStartLimit() {
        serverConfig.setPurgeLimits(null, null);
        assertThat(artifactsDiskCleaner.limitInMegabytes()).isEqualTo(Long.MAX_VALUE);
        serverConfig.setPurgeLimits(20.0, 30.0);
        assertThat(artifactsDiskCleaner.limitInMegabytes()).isEqualTo(fromGigaToMegabytes(20));
        serverConfig.setPurgeLimits(15.0, 30.0);
        assertThat(artifactsDiskCleaner.limitInMegabytes()).isEqualTo(fromGigaToMegabytes(15));
    }

    @Test
    @Timeout(20)
    public void shouldTriggerCleanupWhenLimitReached() throws InterruptedException {
        serverConfig.setPurgeLimits(20.0, 30.0);
        final boolean[] artifactsDeletionTriggered = {false};
        final Thread[] artifactDeleterThread = {null};
        final Semaphore sem = new Semaphore(1);
        sem.acquire();
        artifactsDiskCleaner = new ArtifactsDiskCleaner(sysEnv, goConfigService, diskSpaceChecker, artifactService, stageService, configDbStateRepository) {
            @Override void deleteOldArtifacts() {
                artifactDeleterThread[0] = Thread.currentThread();
                artifactsDeletionTriggered[0] = true;
                sem.release();
            }
        };
        artifactsDiskCleaner.createFailure(new HttpOperationResult(), 10, 100);
        sem.acquire();
        assertThat(artifactsDeletionTriggered[0]).isTrue();
        assertThat(artifactDeleterThread[0]).isNotSameAs(Thread.currentThread());
    }

    @Test
    public void shouldDeleteOldestStagesFirst_untilHasEnoughFreeDisk() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        Stage stageOne = StageMother.passedStageInstance("pipeline", "stage", "build");
        Stage stageTwo = StageMother.passedStageInstance("with-pipeline", "another", "job");
        Stage stageThree = StageMother.passedStageInstance("foo-pipeline", "yet-another", "job1");

        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne, stageTwo, stageThree));
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));

        doAnswer((Answer<Object>) invocation -> {
            when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(6));
            return null;
        }).when(artifactService).purgeArtifactsForStage(stageOne);

        doAnswer((Answer<Object>) invocation -> {
            when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(10));
            return null;
        }).when(artifactService).purgeArtifactsForStage(stageTwo);

        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService).purgeArtifactsForStage(stageOne);
        verify(artifactService).purgeArtifactsForStage(stageTwo);
        verify(configDbStateRepository).flushConfigState();
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldDeleteMultiplePagesOfOldestStagesHavingArtifacts() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        final Stage stageOne = StageMother.passedStageInstance("pipeline", "stage", "build");
        final Stage stageTwo = StageMother.passedStageInstance("with-pipeline", "another", "job");
        final Stage stageThree = StageMother.passedStageInstance("foo-pipeline", "yet-another", "job1");
        final Stage stageFour = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");
        final Stage stageFive = StageMother.passedStageInstance("quux-pipeline", "bar-stage", "baz-job");

        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne, stageTwo));
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));

        doAnswer((Answer<Object>) invocation -> {
            when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageThree, stageFour));
            return null;
        }).when(artifactService).purgeArtifactsForStage(stageTwo);

        doAnswer((Answer<Object>) invocation -> {
            when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageFive));
            return null;
        }).when(artifactService).purgeArtifactsForStage(stageFour);

        doAnswer((Answer<Object>) invocation -> {
            when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(new ArrayList<>());
            return null;
        }).when(artifactService).purgeArtifactsForStage(stageFive);

        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService).purgeArtifactsForStage(stageOne);
        verify(artifactService).purgeArtifactsForStage(stageTwo);
        verify(artifactService).purgeArtifactsForStage(stageThree);
        verify(artifactService).purgeArtifactsForStage(stageFour);
        verify(artifactService).purgeArtifactsForStage(stageFive);

        verify(stageService, times(4)).oldestStagesWithDeletableArtifacts();
        verify(configDbStateRepository, times(4)).flushConfigState();
        verifyNoMoreInteractions(artifactService);
        verifyNoMoreInteractions(stageService);
    }

    @Test
    public void shouldDeleteArtifactDirectoriesOfOldestStagesFirst_whenPurgingArtifactDirectories() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage stageOne = StageMother.passedStageInstance("pipeline", "stage", "build");
        Stage stageTwo = StageMother.passedStageInstance("with-pipeline", "another", "job");
        Stage stageThree = StageMother.passedStageInstance("foo-pipeline", "yet-another", "job1");

        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne, stageTwo, stageThree)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(stageOne, 6);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(stageTwo, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageTwo);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldDeleteArtifactDirectoriesOfStagesWithPurgedArtifactsBeforeAnyOtherStage_whenPurgingArtifactDirectories() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");
        Stage stageTwo = StageMother.passedStageInstance("quux-pipeline", "bar-stage", "baz-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo));
        when(stageService.oldestStagesWithPurgedArtifacts(12)).thenReturn(List.of(purgedStageThree));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne, stageTwo)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageOne, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageTwo, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageThree, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(stageOne, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService, stageService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageThree);
        inOrder.verify(stageService).oldestStagesWithPurgedArtifacts(13);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageOne);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldNotPurgeAnyOtherStage_whenDeletingArtifactDirectoriesOfStagesWithPurgedArtifactsFreesEnoughDisk() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageOne, 6);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageTwo, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldContinueFromLastStageWithPurgedArtifactsThatWasRevisited_whenCleanupIsTriggeredAgain() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithPurgedArtifacts(12)).thenReturn(List.of(purgedStageThree));
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageOne, 6);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageTwo, 10);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageThree, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService, never()).purgeArtifactDirectoriesForStage(purgedStageThree);

        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageThree);
        verify(stageService).oldestStagesWithPurgedArtifacts(0);
        verify(stageService).oldestStagesWithPurgedArtifacts(12);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldRevisitAllStagesWithPurgedArtifacts_afterArtifactsWerePurgedWithoutPurgingArtifactDirectories() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage purgedStage = purgedStage("stage", 11);

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStage));
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStage, 10);

        artifactsDiskCleaner.deleteOldArtifacts();
        verify(stageService).oldestStagesWithPurgedArtifacts(0);

        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        artifactsDiskCleaner.deleteOldArtifacts();
        verify(stageService).oldestStagesWithPurgedArtifacts(0);
        verify(stageService, atLeastOnce()).oldestStagesWithPurgedArtifacts(11);

        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(false);
        artifactsDiskCleaner.deleteOldArtifacts();
        verify(stageService).oldestStagesWithPurgedArtifacts(0);

        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        artifactsDiskCleaner.deleteOldArtifacts();
        verify(stageService, times(2)).oldestStagesWithPurgedArtifacts(0);
        verify(artifactService, times(2)).purgeArtifactDirectoriesForStage(purgedStage);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldGiveUpRevisitingStagesWithPurgedArtifactsThatHaveNothingLeftBehind_soAsNotToHoldUpPurgingOfOtherStages() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        artifactsDiskCleaner = artifactsDiskCleanerThatRevisitsInVainAtMost(2);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithPurgedArtifacts(12)).thenReturn(List.of(purgedStageThree));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(stageOne, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageOne);
        verifyNoMoreInteractions(artifactService);

        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of());
        artifactsDiskCleaner.deleteOldArtifacts();

        verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageThree);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldNotGiveUpRevisitingStagesWithPurgedArtifactsWhileTheyHaveSomethingLeftBehind() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        artifactsDiskCleaner = artifactsDiskCleanerThatRevisitsInVainAtMost(1);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageOne, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageTwo, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageThree, 4);
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(stageOne, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageThree);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageOne);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldCarryOnRevisitingStagesWithPurgedArtifacts_whenThereIsNothingElseLeftToPurge() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        artifactsDiskCleaner = artifactsDiskCleanerThatRevisitsInVainAtMost(1);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage purgedStageThree = purgedStage("yet-another", 13);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne, purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithPurgedArtifacts(11)).thenReturn(List.of(purgedStageTwo, purgedStageThree));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        availableDiskSpaceAfterPurgingArtifactDirectoriesFor(purgedStageThree, 10);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(stageOne);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageTwo);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageThree);
        verifyNoMoreInteractions(artifactService);
    }

    @Test
    public void shouldStopPurgingArtifactDirectories_whenItIsTurnedOffWhileCleanupIsUnderway() {
        serverConfig.setPurgeLimits(5.0, 9.0);
        serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(true);
        Stage purgedStageOne = purgedStage("stage", 11);
        Stage purgedStageTwo = purgedStage("another", 12);
        Stage stageOne = StageMother.passedStageInstance("baz-pipeline", "foo-stage", "bar-job");

        when(stageService.oldestStagesWithPurgedArtifacts(0)).thenReturn(List.of(purgedStageOne));
        when(stageService.oldestStagesWithPurgedArtifacts(11)).thenReturn(List.of(purgedStageTwo));
        when(stageService.oldestStagesWithDeletableArtifacts()).thenReturn(List.of(stageOne)).thenReturn(List.of());
        when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(4));
        doAnswer((Answer<Object>) invocation -> {
            serverConfig.getArtifactConfig().getPurgeSettings().setPurgeArtifactDirectories(false);
            return true;
        }).when(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);

        artifactsDiskCleaner.deleteOldArtifacts();

        InOrder inOrder = inOrder(artifactService);
        inOrder.verify(artifactService).purgeArtifactDirectoriesForStage(purgedStageOne);
        inOrder.verify(artifactService).purgeArtifactsForStage(stageOne);
        verifyNoMoreInteractions(artifactService);
    }

    private ArtifactsDiskCleaner artifactsDiskCleanerThatRevisitsInVainAtMost(int maxStagesToRevisitInVain) {
        return new ArtifactsDiskCleaner(sysEnv, goConfigService, diskSpaceChecker, artifactService, stageService, configDbStateRepository) {
            @Override int maxStagesToRevisitInVain() {
                return maxStagesToRevisitInVain;
            }
        };
    }

    private Stage purgedStage(String stageName, long id) {
        Stage stage = StageMother.passedStageInstance("pipeline", stageName, "job");
        stage.setId(id);
        stage.setArtifactsDeleted(true);
        return stage;
    }

    /**
     * Stubs the stage as having had directories to purge, leaving the given amount of disk space available afterwards
     */
    private void availableDiskSpaceAfterPurgingArtifactDirectoriesFor(Stage stage, long gigabytes) {
        doAnswer((Answer<Object>) invocation -> {
            when(diskSpaceChecker.getUsableSpaceBytes(goConfigService.artifactsDir())).thenReturn(fromGigaToBytes(gigabytes));
            return true;
        }).when(artifactService).purgeArtifactDirectoriesForStage(stage);
    }

    @Test
    public void shouldUseA_NonServerHealthAware_result() {
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        OperationResult operationResult = artifactsDiskCleaner.resultFor(new DiskSpaceOperationResult(serverHealthService));
        assertThat(operationResult).isInstanceOf(ServerHealthStateOperationResult.class);
    }
}
