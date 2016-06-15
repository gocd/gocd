/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service;

import static java.lang.String.format;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands forced unlocking of pipeline by a user
 * 
 * unlock belongs in PipelineLockService but had to be pulled into a separate service
 * because of circular dependency between CachedCurrentActivityService and PipelineLockService
 * (unlock still is in PipelineLockService, the unlock here does necessary validity checks
 * and delegates to PipelineLockService, and PipelineLockService is the only one dealing with
 * locking and unlocking of pipelines)
 */
@Service
public class PipelineUnlockApiService {
    private final GoConfigService goConfigService;
    private final PipelineLockService pipelineLockService;
    private final CurrentActivityService currentActivityService;
    private final SecurityService securityService;
    private final PipelineSqlMapDao pipelineDao;

    @Autowired
    public PipelineUnlockApiService(GoConfigService goConfigService, PipelineLockService pipelineLockService,
                                    CurrentActivityService currentActivityService, SecurityService securityService,
                                    PipelineSqlMapDao pipelineDao) {
        this.goConfigService = goConfigService;
        this.pipelineLockService = pipelineLockService;
        this.currentActivityService = currentActivityService;
        this.securityService = securityService;
        this.pipelineDao = pipelineDao;
    }

    public void unlock(String pipelineName, Username username, OperationResult result) {
        if (canUnlock(pipelineName, username, result)) {
            pipelineLockService.unlock(pipelineName);
            result.ok("pipeline lock released for " + pipelineName);
        }
    }

    public boolean canUnlock(String pipelineName, Username username, OperationResult result) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            String msg = format("pipeline name %s is incorrect", pipelineName);
            result.notFound(msg, msg, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return false;
        }
        if (!securityService.hasOperatePermissionForPipeline(username.getUsername(), pipelineName)) {
            String msg = "user does not have operate permission on the pipeline";
            result.unauthorized(msg, msg, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return false;
        }
        if (!goConfigService.isLockable(pipelineName)) {
            result.notAcceptable(format("no lock exists within the pipeline configuration for %s", pipelineName), HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        StageIdentifier stageIdentifier = pipelineLockService.lockedPipeline(pipelineName);
        if (stageIdentifier == null) {
            result.notAcceptable("lock exists within the pipeline configuration but no pipeline instance is currently in progress", HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        if (currentActivityService.isAnyStageActive(stageIdentifier.pipelineIdentifier())) {
            result.notAcceptable("locked pipeline instance is currently running (one of the stages is in progress)", HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        return true;
    }

}
