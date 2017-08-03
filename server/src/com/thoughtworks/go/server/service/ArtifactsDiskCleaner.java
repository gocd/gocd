/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.util.List;

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;

public class ArtifactsDiskCleaner extends DiskSpaceChecker {
    private static final Logger LOGGER = Logger.getLogger(ArtifactsDiskCleaner.class);
    private final Object triggerCleanup = new Object();
    private final Thread cleaner;
    private final ArtifactsService artifactService;
    private final StageService stageService;
    private final ConfigDbStateRepository configDbStateRepository;

    public ArtifactsDiskCleaner(SystemEnvironment systemEnvironment, GoConfigService goConfigService, final SystemDiskSpaceChecker diskSpaceChecker, ArtifactsService artifactService,
                                StageService stageService, ConfigDbStateRepository configDbStateRepository) {
        super(null, systemEnvironment, goConfigService.artifactsDir(), goConfigService, ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID, diskSpaceChecker);
        this.artifactService = artifactService;
        this.stageService = stageService;
        this.configDbStateRepository = configDbStateRepository;
        cleaner = new Thread(() -> {
            try {
                while (true) {
                    synchronized (triggerCleanup) {
                        triggerCleanup.wait();
                    }
                    deleteOldArtifacts();
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Artifact disk cleanup task aborted. Error encountered: '%s'", e.getMessage()));//logging not tested
                throw new RuntimeException(e);
            }
        });
        cleaner.start();
    }

    void deleteOldArtifacts() {
        ServerConfig serverConfig = goConfigService.serverConfig();
        Double requiredSpaceInGb = serverConfig.getPurgeUpto();
        if (serverConfig.isArtifactPurgingAllowed()) {
            double requiredSpace = requiredSpaceInGb * GoConstants.GIGA_BYTE;
            LOGGER.info(String.format("Clearing old artifacts as the disk space is low. Current space: '%s'. Need to clear till we hit: '%s'.", availableSpace(), requiredSpace));
            List<Stage> stages;
            int numberOfStagesPurged = 0;
            do {
                configDbStateRepository.flushConfigState();
                stages = stageService.oldestStagesWithDeletableArtifacts();
                for (Stage stage : stages) {
                    if (availableSpace() > requiredSpace) {
                        break;
                    }
                    numberOfStagesPurged++;
                    artifactService.purgeArtifactsForStage(stage);
                }
            } while ((availableSpace() < requiredSpace) && !stages.isEmpty());
            if (availableSpace() < requiredSpace) {
                LOGGER.warn("Ran out of stages to clear artifacts from but the disk space is still low");
            }
            LOGGER.info(String.format("Finished clearing old artifacts. Deleted artifacts for '%s' stages. Current space: '%s'", numberOfStagesPurged, availableSpace()));
        }
    }

    protected void createFailure(OperationResult result, long size, long availableSpace) {
        synchronized (triggerCleanup) {
            triggerCleanup.notify();
        }
    }

    protected SendEmailMessage createEmail() {
        throw new UnsupportedOperationException("Disk cleaner does not send messages");
    }

    protected long limitInMb() {
        ServerConfig serverConfig = goConfigService.serverConfig();
        return serverConfig.isArtifactPurgingAllowed() ? new Double(serverConfig.getPurgeStart() * GoConstants.MEGABYTES_IN_GIGABYTE).longValue() : Integer.MAX_VALUE;
    }

    @Override public OperationResult resultFor(OperationResult result) {
        return new ServerHealthStateOperationResult();
    }
}
