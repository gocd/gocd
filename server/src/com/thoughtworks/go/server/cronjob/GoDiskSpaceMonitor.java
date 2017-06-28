/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.service.ArtifactsDiskCleaner;
import com.thoughtworks.go.server.service.ArtifactsDiskSpaceFullChecker;
import com.thoughtworks.go.server.service.ArtifactsDiskSpaceWarningChecker;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConfigDbStateRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.DatabaseDiskSpaceWarningChecker;
import com.thoughtworks.go.server.service.DiskSpaceChecker;
import com.thoughtworks.go.server.service.EmailSender;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.SystemDiskSpaceChecker;
import com.thoughtworks.go.server.service.DatabaseDiskSpaceFullChecker;
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
        checkers = new DiskSpaceChecker[]{
                new ArtifactsDiskSpaceFullChecker(systemEnvironment, emailSender, goConfigService, systemDiskSpaceChecker),
                new ArtifactsDiskSpaceWarningChecker(systemEnvironment, emailSender, goConfigService, systemDiskSpaceChecker, serverHealthService),
                new DatabaseDiskSpaceFullChecker(emailSender, systemEnvironment, goConfigService, systemDiskSpaceChecker),
                new DatabaseDiskSpaceWarningChecker(emailSender, systemEnvironment, goConfigService, systemDiskSpaceChecker, serverHealthService),
                new ArtifactsDiskCleaner(systemEnvironment, goConfigService, systemDiskSpaceChecker, artifactsService, stageService, configDbStateRepository)};
    }

    //Note: This method is called from a Spring timer task
    public void onTimer() {
        OperationResult result = new DiskSpaceOperationResult(serverHealthService);
        try {
            boolean outOfDisk = false;
            for (DiskSpaceChecker checker : checkers) {
                OperationResult callResult = checker.resultFor(result);
                checker.check(callResult);
                outOfDisk = outOfDisk || !result.canContinue();
            }
            lowOnDisk = outOfDisk;
        } catch (Exception e) {
            LOG.error("Error occured during checking filesystems low disk space", e);
        }
    }

    public boolean isLowOnDisk() {
        return lowOnDisk;
    }
}
