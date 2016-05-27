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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands how/whether to lock/unlock a pipeline instance
 */
@Service
public class PipelineLockService implements ConfigChangedListener {
    private final GoConfigService goConfigService;
    private final PipelineSqlMapDao pipelineDao;

    @Autowired
    public PipelineLockService(GoConfigService goConfigService, PipelineSqlMapDao pipelineDao) {
        this.goConfigService = goConfigService;
        this.pipelineDao = pipelineDao;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                for (String lockedPipeline : pipelineDao.lockedPipelines()) {
                    if (pipelineConfig.name().equals(new CaseInsensitiveString(lockedPipeline)) && !pipelineConfig.isLock()) {
                        pipelineDao.unlockPipeline(lockedPipeline);
                        break;
                    }
                }
            }
        };
    }

    public void lockIfNeeded(Pipeline pipeline) {
        if (goConfigService.isLockable(pipeline.getName())) {
            pipelineDao.lockPipeline(pipeline);
        }
    }

    public boolean isLocked(String pipelineName) {
        return lockedPipeline(pipelineName) != null;
    }

    public StageIdentifier lockedPipeline(String pipelineName) {
        return pipelineDao.lockedPipeline(pipelineName);
    }

    public void unlock(String pipelineName) {
        pipelineDao.unlockPipeline(pipelineName);
    }

    public boolean canScheduleStageInPipeline(PipelineIdentifier pipeline) {
        if (!goConfigService.isLockable(pipeline.getName())) {
            return true;
        }
        StageIdentifier locked = lockedPipeline(pipeline.getName());
        return locked == null || locked.pipelineIdentifier().equals(pipeline);
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        for (String lockedPipeline : pipelineDao.lockedPipelines()) {
            if (!newCruiseConfig.hasPipelineNamed(new CaseInsensitiveString(lockedPipeline)) || !newCruiseConfig.isPipelineLocked(lockedPipeline)) {
                pipelineDao.unlockPipeline(lockedPipeline);
            }
        }
    }
}
