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
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.FileSizeUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;

public class ArtifactsDiskCleaner extends DiskSpaceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsDiskCleaner.class);
    private final Semaphore triggerCleanup = new Semaphore(0);
    private final ArtifactsService artifactService;
    private final StageService stageService;
    private final ConfigDbStateRepository configDbStateRepository;
    private long lastRevisitedStageId;

    public ArtifactsDiskCleaner(SystemEnvironment systemEnvironment, GoConfigService goConfigService, final SystemDiskSpaceChecker diskSpaceChecker, ArtifactsService artifactService,
                                StageService stageService, ConfigDbStateRepository configDbStateRepository) {
        super(null, systemEnvironment, goConfigService.artifactsDir(), goConfigService, ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID, diskSpaceChecker);
        this.artifactService = artifactService;
        this.stageService = stageService;
        this.configDbStateRepository = configDbStateRepository;

        Thread.ofPlatform()
            .name("goArtifactsDiskCleaner")
            .daemon(true)
            .start(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        triggerCleanup.acquire();
                        triggerCleanup.drainPermits(); // In case signal multiple times while cleaning
                        deleteOldArtifacts();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
    }

    void deleteOldArtifacts() {
        ServerConfig serverConfig = goConfigService.serverConfig();
        if (!serverConfig.isArtifactPurgingAllowed()) {
            return;
        }
        try {
            double requiredSpaceBytes = FileSizeUtils.fromGigaToBytes(serverConfig.getPurgeUptoDiskSpaceInGigabytes().longValue());
            LOGGER.info("Clearing old artifacts as the disk space is low. Current space: '{}'. Need to clear till we hit: '{}'.", availableSpaceBytes(), requiredSpaceBytes);
            purgeDirectoriesOfStagesWithPurgedArtifacts(requiredSpaceBytes, maxStagesToRevisitInVain());
            List<Stage> stages;
            int numberOfStagesPurged = 0;
            do {
                configDbStateRepository.flushConfigState();
                stages = stageService.oldestStagesWithDeletableArtifacts();
                for (Stage stage : stages) {
                    if (availableSpaceBytes() > requiredSpaceBytes) {
                        break;
                    }
                    numberOfStagesPurged++;
                    if (shouldPurgeArtifactDirectories()) {
                        artifactService.purgeArtifactDirectoriesForStage(stage);
                    } else {
                        artifactService.purgeArtifactsForStage(stage);
                    }
                }
            } while (availableSpaceBytes() < requiredSpaceBytes && !stages.isEmpty());

            if (availableSpaceBytes() < requiredSpaceBytes) {
                // There is nothing else left to purge, so it no longer holds anything up to revisit every last stage
                purgeDirectoriesOfStagesWithPurgedArtifacts(requiredSpaceBytes, Integer.MAX_VALUE);
            }

            if (availableSpaceBytes() < requiredSpaceBytes) {
                LOGGER.warn("Ran out of stages to clear artifacts from but the disk space is still low");
            }
            LOGGER.info("Finished clearing old artifacts. Deleted artifacts for '{}' stages. Current space: '{}'", numberOfStagesPurged, availableSpaceBytes());
        } catch (Throwable e) {
            LOGGER.error("Artifact disk cleanup task aborted. Error encountered: '{}'", e.getMessage());//logging not tested
            throw new RuntimeException(e);
        }
    }

    /**
     * Stages that had their artifacts purged earlier, or by an older version of GoCD, may still have console logs and
     * directories on disk. Artifacts are purged oldest stage first, so these tend to be the oldest stages of all and
     * are dealt with before anything else is purged.
     * <p>
     * The last stage to have been revisited is remembered, so that the same stages are not checked all over again each
     * time the cleanup is triggered. That is forgotten when the server restarts though, so to keep a long history of
     * stages which turn out to have nothing left on disk from holding up the purging of artifacts, this gives up after
     * revisiting the given number of such stages in vain and carries on from there the next time.
     */
    private void purgeDirectoriesOfStagesWithPurgedArtifacts(double requiredSpaceBytes, int maxStagesToRevisitInVain) {
        if (!shouldPurgeArtifactDirectories()) {
            lastRevisitedStageId = 0; // Stages purged in the meantime leave their directories behind, so all have to be revisited
            return;
        }
        configDbStateRepository.flushConfigState();
        List<Stage> stages;
        int numberOfStagesPurged = 0;
        int numberOfStagesRevisitedInVain = 0;
        do {
            stages = stageService.oldestStagesWithPurgedArtifacts(lastRevisitedStageId);
            for (Stage stage : stages) {
                if (availableSpaceBytes() > requiredSpaceBytes || numberOfStagesRevisitedInVain >= maxStagesToRevisitInVain) {
                    break;
                }
                if (artifactService.purgeArtifactDirectoriesForStage(stage)) {
                    numberOfStagesPurged++;
                } else {
                    numberOfStagesRevisitedInVain++;
                }
                lastRevisitedStageId = stage.getId();
            }
        } while (availableSpaceBytes() < requiredSpaceBytes && numberOfStagesRevisitedInVain < maxStagesToRevisitInVain && !stages.isEmpty() && shouldPurgeArtifactDirectories());

        if (numberOfStagesPurged > 0) {
            LOGGER.info("Deleted console logs and directories left behind for '{}' stages that had their artifacts cleared earlier. Current space: '{}'", numberOfStagesPurged, availableSpaceBytes());
        }
    }

    int maxStagesToRevisitInVain() {
        return 10_000;
    }

    // Consulted as the cleanup goes along rather than once up front, so that turning it off also stops a cleanup that is underway
    private boolean shouldPurgeArtifactDirectories() {
        return goConfigService.serverConfig().isArtifactDirectoryPurgingAllowed();
    }

    @Override
    protected void createFailure(OperationResult result, long limitMegabytes, long availableSpace) {
        triggerCleanup.release();
    }

    @Override
    protected SendEmailMessage createEmail() {
        throw new UnsupportedOperationException("Disk cleaner does not send messages");
    }

    @Override
    protected long limitInMegabytes() {
        ServerConfig serverConfig = goConfigService.serverConfig();
        return serverConfig.isArtifactPurgingAllowed() ? FileSizeUtils.fromGigaToMegabytes(serverConfig.getPurgeStartDiskSpaceInGigabytes().longValue()) : Long.MAX_VALUE;
    }

    @Override
    public OperationResult resultFor(OperationResult result) {
        return new ServerHealthStateOperationResult();
    }
}
