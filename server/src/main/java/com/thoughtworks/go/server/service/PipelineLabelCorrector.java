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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PipelineLabelCorrector {
    private GoConfigService goConfigService;
    private PipelineSqlMapDao pipelineSqlMapDao;
    private ServerHealthService serverHealthService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineLabelCorrector.class);

    @Autowired
    public PipelineLabelCorrector(GoConfigService goConfigService, PipelineSqlMapDao pipelineSqlMapDao,
                                  ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.serverHealthService = serverHealthService;
    }

    public void correctPipelineLabelCountEntries() {
        List<String> pipelinesWithMultipleEntriesForLabelCount = pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount();
        if (pipelinesWithMultipleEntriesForLabelCount.isEmpty()) return;

        LOGGER.warn("Duplicate entries in pipelineLabelCounts exist for the following pipelines {}. Will attempt to clean them up.",
                StringUtils.join(pipelinesWithMultipleEntriesForLabelCount.toArray()));

        Map<CaseInsensitiveString, PipelineConfig> pipelineConfigHashMap = goConfigService.cruiseConfig().pipelineConfigsAsMap();
        pipelinesWithMultipleEntriesForLabelCount.stream().forEach(pipelineWithIssue -> {
            CaseInsensitiveString key = new CaseInsensitiveString(pipelineWithIssue);
            if (pipelineConfigHashMap.containsKey(key)) {
                PipelineConfig pipelineConfig = pipelineConfigHashMap.get(key);
                String pipelineNameAsSavedInConfig = pipelineConfig.name().toString();
                LOGGER.warn("Deleting duplicate entries in pipelineLabelCounts for pipeline {} currently in config .", pipelineWithIssue);
                pipelineSqlMapDao.deleteOldPipelineLabelCountForPipelineInConfig(pipelineNameAsSavedInConfig);
            } else {
                LOGGER.warn("Deleting duplicate entries in pipelineLabelCounts for pipeline {} currently not in config .", pipelineWithIssue);
                pipelineSqlMapDao.deleteOldPipelineLabelCountForPipelineCurrentlyNotInConfig(pipelineWithIssue);
            }
        });

        List<String> pipelineNamesWithMultipleEntriesForLabelCount = pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount();
        if (!pipelineNamesWithMultipleEntriesForLabelCount.isEmpty()) {
            String message = String.format("Duplicate entries in pipelineLabelCounts still exist for the following pipelines %s.",
                    StringUtils.join(pipelineNamesWithMultipleEntriesForLabelCount.toArray()));
            LOGGER.error(message);
            serverHealthService.update(ServerHealthState.error("Data Error: pipeline operations will fail", message,
                    HealthStateType.general(HealthStateScope.forDuplicatePipelineLabel())));
        }
    }
}
