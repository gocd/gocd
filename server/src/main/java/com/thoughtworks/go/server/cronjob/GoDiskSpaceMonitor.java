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
package com.thoughtworks.go.server.cronjob;

import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.DiskSpaceOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoDiskSpaceMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(GoDiskSpaceMonitor.class);
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;
    private EmailSender emailSender;
    private SystemDiskSpaceChecker systemDiskSpaceChecker;
    private ArtifactsService artifactsService;
    private StageService stageService;
    private ConfigDbStateRepository configDbStateRepository;
    private DiskSpaceChecker[] checkers;
    private volatile boolean lowOnDisk;
    private DatabaseDiskSpaceFullChecker databaseDiskSpaceFullChecker;
    private ArtifactsDiskSpaceFullChecker artifactsDiskSpaceFullChecker;

    @Autowired
    public GoDiskSpaceMonitor(GoConfigService goConfigService,
                              SystemEnvironment systemEnvironment,
                              ServerHealthService serverHealthService,
                              EmailSender emailSender,
                              ArtifactsService artifactsService,
                              StageService stageService,
                              ConfigDbStateRepository configDbStateRepository) {
        this(goConfigService, systemEnvironment, serverHealthService, emailSender, new SystemDiskSpaceChecker(), artifactsService, stageService, configDbStateRepository);
    }

    public GoDiskSpaceMonitor(GoConfigService goConfigService, SystemEnvironment systemEnvironment, ServerHealthService serverHealthService, EmailSender emailSender,
                              SystemDiskSpaceChecker systemDiskSpaceChecker, ArtifactsService artifactsService, StageService stageService, ConfigDbStateRepository configDbStateRepository) {
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.serverHealthService = serverHealthService;
        this.emailSender = emailSender;
        this.systemDiskSpaceChecker = systemDiskSpaceChecker;
        this.artifactsService = artifactsService;
        this.stageService = stageService;
        this.configDbStateRepository = configDbStateRepository;
    }

    public void initialize() {
        databaseDiskSpaceFullChecker = new DatabaseDiskSpaceFullChecker(emailSender, systemEnvironment, goConfigService, systemDiskSpaceChecker);
        artifactsDiskSpaceFullChecker = new ArtifactsDiskSpaceFullChecker(systemEnvironment, emailSender, goConfigService, systemDiskSpaceChecker);

        checkers = new DiskSpaceChecker[]{
                artifactsDiskSpaceFullChecker,
                new ArtifactsDiskSpaceWarningChecker(systemEnvironment, emailSender, goConfigService, systemDiskSpaceChecker, serverHealthService),
                databaseDiskSpaceFullChecker,
                new DatabaseDiskSpaceWarningChecker(emailSender, systemEnvironment, goConfigService, systemDiskSpaceChecker, serverHealthService),
                new ArtifactsDiskCleaner(systemEnvironment, goConfigService, systemDiskSpaceChecker, artifactsService, stageService, configDbStateRepository)};
    }

    //Note: This method is called from a Spring timer task
    public void onTimer() {
        lowOnDisk = lowOnDisk(lowOnDisk, new DiskSpaceOperationResult(serverHealthService), checkers);
    }

    private boolean lowOnDisk(boolean currentlyLowOnDisk, OperationResult result, DiskSpaceChecker... checkers) {
        try {
            boolean outOfDisk = false;
            for (DiskSpaceChecker checker : checkers) {
                OperationResult callResult = checker.resultFor(result);
                checker.check(callResult);
                outOfDisk = outOfDisk || !result.canContinue();
            }
            return outOfDisk;
        } catch (Exception e) {
            LOG.error("Error occured during checking filesystems low disk space", e);
        }
        return currentlyLowOnDisk;
    }

    public boolean isLowOnDisk() {
        return lowOnDisk;
    }

    public void checkIfOutOfDisk(OperationResult result) {
        lowOnDisk(false, result, databaseDiskSpaceFullChecker, artifactsDiskSpaceFullChecker);
    }
}
