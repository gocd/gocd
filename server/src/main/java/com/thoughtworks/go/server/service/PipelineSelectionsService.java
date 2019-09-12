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
import com.thoughtworks.go.server.dao.PipelineSelectionDao;
import com.thoughtworks.go.server.domain.user.Filters;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PipelineSelectionsService {
    private final Clock clock;
    private final GoConfigService goConfigService;
    private final PipelineSelectionDao pipelineSelectionDao;

    @Autowired
    public PipelineSelectionsService(GoConfigService goConfigService, Clock clock, PipelineSelectionDao pipelineSelectionDao) {
        this.goConfigService = goConfigService;
        this.clock = clock;
        this.pipelineSelectionDao = pipelineSelectionDao;
    }

    public PipelineSelections load(String id, Long userId) {
        PipelineSelections pipelineSelections = loadByIdOrUserId(id, userId);

        if (pipelineSelections == null) {
            pipelineSelections = PipelineSelections.ALL;
        }

        return pipelineSelections;
    }

    public long save(String id, Long userId, Filters filters) {
        PipelineSelections pipelineSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);
        pipelineSelections.update(filters, clock.currentTimestamp(), userId);

        return pipelineSelectionDao.saveOrUpdate(pipelineSelections);
    }

    public void update(String id, Long userId, CaseInsensitiveString pipelineToAdd) {
        PipelineSelections currentSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);

        if (currentSelections.ensurePipelineVisible(pipelineToAdd)) {
            pipelineSelectionDao.saveOrUpdate(currentSelections);
        }
    }

    private PipelineSelections findOrCreateCurrentPipelineSelectionsFor(String id, Long userId) {
        PipelineSelections pipelineSelections = loadByIdOrUserId(id, userId);

        if (pipelineSelections == null) {
            return new PipelineSelections(Filters.defaults(), clock.currentTimestamp(), userId);
        }

        return pipelineSelections;
    }

    private PipelineSelections loadByIdOrUserId(String id, Long userId) {
        return goConfigService.isSecurityEnabled() ? pipelineSelectionDao.findPipelineSelectionsByUserId(userId) : pipelineSelectionDao.findPipelineSelectionsById(id);
    }
}
