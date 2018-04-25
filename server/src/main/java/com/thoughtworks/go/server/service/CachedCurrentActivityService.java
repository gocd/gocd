/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.PipelineIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CachedCurrentActivityService implements CurrentActivityService {

    private StageService stageService;

    @Autowired
    public CachedCurrentActivityService(StageService stageService) {
        this.stageService = stageService;
    }

    public boolean isStageActive(String pipelineName, String stageName) {
        return stageService.isStageActive(pipelineName, stageName);
    }

    public boolean isAnyStageActive(PipelineIdentifier pipelineIdentifier) {
        return stageService.isAnyStageActiveForPipeline(pipelineIdentifier.getName(), pipelineIdentifier.getCounter());
    }

}
