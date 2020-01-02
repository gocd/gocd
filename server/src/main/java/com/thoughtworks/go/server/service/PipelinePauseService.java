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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.serverhealth.HealthStateScope.forPipeline;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbiddenForPipeline;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;

@Service
public class PipelinePauseService {

    private PipelineSqlMapDao pipelineSqlMapDao;
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinePauseService.class);
    private List<PipelinePauseChangeListener> listeners = new ArrayList<>();

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
        if (isPipelinePaused(pipelineName)) {
            result.conflict("Failed to pause pipeline '" +pipelineName + "'. Pipeline '" +pipelineName + "' is already paused.");
            return;
        }
        try {
            pausePipeline(pipelineName, pauseCause, pausedBy);
            result.setMessage("Pipeline '" + pipelineName + "' paused successfully.");
        } catch (Exception e) {
            LOGGER.error("[Pipeline Pause] Failed to pause pipeline", e);
            result.internalServerError("Server error occured. Check log for details.");
        }
    }

    private boolean isPipelinePaused(String pipelineName) {
        return pipelineSqlMapDao.pauseState(pipelineName).isPaused();
    }

    public void unpause(String pipelineName) {
        unpause(pipelineName, SessionUtils.currentUsername(), new DefaultLocalizedOperationResult());
    }

    public void unpause(String pipelineName, Username unpausedBy, LocalizedOperationResult result) {
        String unpauseByUserName = unpausedBy == null ? "" : unpausedBy.getUsername().toString();
        if (pipelineDoesNotExist(pipelineName, result) || notAuthorized(pipelineName, unpauseByUserName, result)) {
            return;
        }
        if (!isPipelinePaused(pipelineName)) {
            result.conflict("Failed to unpause pipeline '" + pipelineName + "'. Pipeline '" + pipelineName + "' is already unpaused.");
            return;
        }
        try {
            unpausePipeline(pipelineName, unpausedBy);
            result.setMessage("Pipeline '" + pipelineName + "' unpaused successfully.");
        } catch (Exception e) {
            LOGGER.error("[Pipeline Unpause] Failed to unpause pipeline", e);
            result.internalServerError("Server error occured. Check log for details.");
        }
    }

    public void registerListener(PipelinePauseChangeListener pipelinePauseChangeListener) {
        listeners.add(pipelinePauseChangeListener);
    }

    public PipelinePauseInfo pipelinePauseInfo(String pipelineName) {
        PipelinePauseInfo pipelinePauseInfo = pipelineSqlMapDao.pauseState(pipelineName);
        return pipelinePauseInfo == null ? PipelinePauseInfo.notPaused() : pipelinePauseInfo;
    }

    public boolean isPaused(String pipelineName) {
        return pipelinePauseInfo(pipelineName).isPaused();
    }

    private void pausePipeline(String pipelineName, String pauseCause, Username pauseBy) {
        String mutexPipelineName = mutexForPausePipeline(pipelineName);
        synchronized (mutexPipelineName) {
            String sanitizedPauseCause = pauseCause.substring(0, Math.min(255, pauseCause.length()));
            String pauseByDisplayName = pauseBy.getDisplayName();
            String sanitizedPauseBy = pauseByDisplayName.substring(0, Math.min(255, pauseByDisplayName.length()));
            pipelineSqlMapDao.pause(pipelineName, sanitizedPauseCause, sanitizedPauseBy);
            LOGGER.info("[Pipeline Pause] Pipeline [{}] is paused by [{}] because [{}]", pipelineName, pauseBy, pauseCause);
            notifyListeners(PipelinePauseChangeListener.Event.pause(pipelineName, pauseBy));
        }
    }

    private boolean notAuthorized(String pipelineName, String pauseBy, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(pauseBy), cruiseConfig.findGroupOfPipeline(pipelineConfig).getGroup())) {
            return false;
        }
        result.forbidden(LocalizedMessage.forbiddenToEditPipeline(pipelineName), forbiddenForPipeline(pipelineName));
        return true;
    }

    private boolean pipelineDoesNotExist(String pipelineName, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        if (cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            return false;
        }
        result.notFound(EntityType.Pipeline.notFoundMessage(pipelineName), general(forPipeline(pipelineName)));
        return true;
    }

    private void unpausePipeline(String pipelineName, Username unpausedBy) {
        String mutextPipelineName = mutexForPausePipeline(pipelineName);
        synchronized (mutextPipelineName) {
            pipelineSqlMapDao.unpause(pipelineName);
            LOGGER.info("[Pipeline Unpause] Pipeline [{}] is unpaused by [{}]", pipelineName, unpausedBy);
            notifyListeners(PipelinePauseChangeListener.Event.unPause(pipelineName, unpausedBy));
        }
    }

    /*
     * Mutex shared between PipelinePauseService and PipelineService. This is to avoid constraint violation when
     * updateCounter() and pause() are trying to insert pipeline row if one doesn't exist
     */
    public static String mutexForPausePipeline(String pipelineName) {
        return (PipelineSqlMapDao.class.getName() + "_mutexForPausePipeline_" + pipelineName).intern();
    }

    private void notifyListeners(PipelinePauseChangeListener.Event event) {
        for (PipelinePauseChangeListener listener : listeners) {
            try {
                LOGGER.debug("START  Notifying listener ({}) of event: {}", listener, event);

                listener.pauseStatusChanged(event);

                LOGGER.debug("FINISH Notifying listener ({}) of event: {}", listener, event);
            } catch (Exception e) {
                LOGGER.warn("Failed to notify listener ({}) of event: {}", listener, event);
            }
        }
    }
}
