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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PipelinePauseService {

    private PipelineSqlMapDao pipelineSqlMapDao;
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinePauseService.class);

    @Autowired
    public PipelinePauseService(PipelineSqlMapDao pipelineSqlMapDao, GoConfigService goConfigService, SecurityService securityService) {
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public void pause(String pipelineName, String pauseCause, Username userName) {
        pause(pipelineName, pauseCause, userName, new DefaultLocalizedOperationResult());
    }

    public void pause(String pipelineName, String pauseCause, Username pausedBy, LocalizedOperationResult result) {
        String pauseByUserName = pausedBy == null ? "" : pausedBy.getUsername().toString();
        if (pipelineDoesNotExist(pipelineName, result) || notAuthorized(pipelineName, pauseByUserName, result)) {
            return;
        }
        if (StringUtils.isBlank(pauseCause)) {
            pauseCause = "";
        }
        try {
            pausePipeline(pipelineName, pauseCause, pausedBy);
        } catch (Exception e) {
            LOGGER.error("[Pipeline Pause] Failed to pause pipeline", e);
            result.internalServerError(LocalizedMessage.string("INTERNAL_SERVER_ERROR"));
        }
    }

    private void pausePipeline(String pipelineName, String pauseCause, Username pauseBy) {
        String mutexPipelineName = mutexForPausePipeline(pipelineName);
        synchronized (mutexPipelineName) {
            String sanitizedPauseCause = pauseCause.substring(0, Math.min(255, pauseCause.length()));
            String pauseByDisplayName = pauseBy.getDisplayName();
            String sanitizedPauseBy = pauseByDisplayName.substring(0, Math.min(255, pauseByDisplayName.length()));
            pipelineSqlMapDao.pause(pipelineName, sanitizedPauseCause, sanitizedPauseBy);
            LOGGER.info("[Pipeline Pause] Pipeline [{}] is paused by [{}] because [{}]", pipelineName, pauseBy, pauseCause);
        }
    }

    private boolean notAuthorized(String pipelineName, String pauseBy, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(pauseBy), cruiseConfig.findGroupOfPipeline(pipelineConfig).getGroup())) {
            return false;
        }
        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", pipelineName), HealthStateType.unauthorisedForPipeline(pipelineName));
        return true;
    }

    private boolean pipelineDoesNotExist(String pipelineName, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        if (cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            return false;
        }
        result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "pipeline", pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
        return true;
    }

    public void unpause(String pipelineName) {
        unpause(pipelineName, UserHelper.getUserName(), new DefaultLocalizedOperationResult());
    }

    public void unpause(String pipelineName, Username unpausedBy, LocalizedOperationResult result) {
        String unpauseByUserName = unpausedBy == null ? "" : unpausedBy.getUsername().toString();
        if (pipelineDoesNotExist(pipelineName, result) || notAuthorized(pipelineName, unpauseByUserName, result)) {
            return;
        }
        try {
            unpausePipeline(pipelineName, unpausedBy);
        } catch (Exception e) {
            LOGGER.error("[Pipeline Unpause] Failed to unpause pipeline", e);
            result.internalServerError(LocalizedMessage.string("INTERNAL_SERVER_ERROR"));
        }
    }

    private void unpausePipeline(String pipelineName, Username unpausedBy) {
        String mutextPipelineName = mutexForPausePipeline(pipelineName);
        synchronized (mutextPipelineName) {
            pipelineSqlMapDao.unpause(pipelineName);
            LOGGER.info("[Pipeline Unpause] Pipeline [{}] is unpaused by [{}]", pipelineName, unpausedBy);
        }
    }

    public PipelinePauseInfo pipelinePauseInfo(String pipelineName) {
        PipelinePauseInfo pipelinePauseInfo = pipelineSqlMapDao.pauseState(pipelineName);
        return pipelinePauseInfo == null ? PipelinePauseInfo.notPaused() : pipelinePauseInfo;
    }

    public boolean isPaused(String pipelineName) {
        return pipelinePauseInfo(pipelineName).isPaused();
    }

    /*
     * Mutex shared between PipelinePauseService and PipelineService. This is to avoid constraint violation when
     * updateCounter() and pause() are trying to insert pipeline row if one doesn't exist
     */
    public static String mutexForPausePipeline(String pipelineName) {
        return (PipelineSqlMapDao.class.getName() + "_mutexForPausePipeline_" + pipelineName).intern();
    }
}
