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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PipelineSelectionsService {
    private final Clock clock;
    private final PipelineRepository pipelineRepository;
    private final GoConfigService goConfigService;

    @Autowired
    public PipelineSelectionsService(PipelineRepository pipelineRepository, GoConfigService goConfigService, Clock clock) {
        this.pipelineRepository = pipelineRepository;
        this.goConfigService = goConfigService;
        this.clock = clock;
    }

    public PipelineSelections getPersistedSelectedPipelines(String id, Long userId) {
        PipelineSelections pipelineSelections = getPersistedPipelineSelections(id, userId);
        if (pipelineSelections == null) {
            pipelineSelections = PipelineSelections.ALL;
        }
        return pipelineSelections;
    }

    public PipelineSelections getSelectedPipelines(String id, Long userId){
        PipelineSelections persistedPipelineSelections = getPersistedPipelineSelections(id, userId);
        if(persistedPipelineSelections.isBlacklist()){
            List<String> invertedPipelineSelections = invertSelections(persistedPipelineSelections.pipelineList());
            return new PipelineSelections(invertedPipelineSelections, persistedPipelineSelections.lastUpdated(), persistedPipelineSelections.userId(), persistedPipelineSelections.isBlacklist());
        }
        return persistedPipelineSelections;
    }

    public long persistSelectedPipelines(String id, Long userId, List<String> selectedPipelines, boolean isBlacklist) {
        PipelineSelections pipelineSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);

        if (isBlacklist) {
            List<String> unselectedPipelines = invertSelections(selectedPipelines);
            pipelineSelections.update(unselectedPipelines, clock.currentTime(), userId, isBlacklist);
        } else {
            pipelineSelections.update(selectedPipelines, clock.currentTime(), userId, isBlacklist);
        }

        return pipelineRepository.saveSelectedPipelines(pipelineSelections);
    }

    private PipelineSelections findOrCreateCurrentPipelineSelectionsFor(String id, Long userId) {
        PipelineSelections pipelineSelections = goConfigService.isSecurityEnabled() ? pipelineRepository.findPipelineSelectionsByUserId(userId) : pipelineRepository.findPipelineSelectionsById(id);
        if (pipelineSelections == null) {
            pipelineSelections = new PipelineSelections(new ArrayList<String>(), clock.currentTime(), userId, true);
        }
        return pipelineSelections;
    }

    private List<String> invertSelections(List<String> selectedPipelines) {
        List<String> unselectedPipelines = new ArrayList<>();
        List<PipelineConfig> pipelineConfigList = goConfigService.getAllPipelineConfigs();
        for (PipelineConfig pipelineConfig : pipelineConfigList) {
            String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
            if (!selectedPipelines.contains(pipelineName)) {
                unselectedPipelines.add(pipelineName);
            }
        }
        return unselectedPipelines;
    }

    private PipelineSelections getPersistedPipelineSelections(String id, Long userId) {
        if (goConfigService.isSecurityEnabled()) {
            return pipelineRepository.findPipelineSelectionsByUserId(userId);
        } else {
            return pipelineRepository.findPipelineSelectionsById(id);
        }
    }

    public void updateUserPipelineSelections(String id, Long userId, CaseInsensitiveString pipelineToAdd) {
        PipelineSelections currentSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);
        if (!currentSelections.isBlacklist()) {
            currentSelections.addPipelineToSelections(pipelineToAdd);
            pipelineRepository.saveSelectedPipelines(currentSelections);
        }
    }

}
