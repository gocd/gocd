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
                    artifactService.purgeArtifactsForStage(stage);
                }
            } while (availableSpaceBytes() < requiredSpaceBytes && !stages.isEmpty());

            if (availableSpaceBytes() < requiredSpaceBytes) {
                LOGGER.warn("Ran out of stages to clear artifacts from but the disk space is still low");
            }
            LOGGER.info("Finished clearing old artifacts. Deleted artifacts for '{}' stages. Current space: '{}'", numberOfStagesPurged, availableSpaceBytes());
        } catch (Throwable e) {
            LOGGER.error("Artifact disk cleanup task aborted. Error encountered: '{}'", e.getMessage());//logging not tested
            throw new RuntimeException(e);
        }
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
