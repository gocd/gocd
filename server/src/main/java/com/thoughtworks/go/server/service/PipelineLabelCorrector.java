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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
public class PipelineLabelCorrector {
    private GoConfigService goConfigService;
    private PipelineSqlMapDao pipelineSqlMapDao;

    @Autowired
    public PipelineLabelCorrector(GoConfigService goConfigService, PipelineSqlMapDao pipelineSqlMapDao) {
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
    }

    public void correctPipelineLabelCountEntries() {
        List<String> pipelinesWithMultipleEntriesForLabelCount = pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount();
        if (pipelinesWithMultipleEntriesForLabelCount.isEmpty()) return;

        List<PipelineConfig> allPipelineConfigs = goConfigService.getAllPipelineConfigs();

        pipelinesWithMultipleEntriesForLabelCount.stream().forEach(new Consumer<String>() {
            @Override
            public void accept(String pipelineWithIssue) {
                allPipelineConfigs.stream().filter(new Predicate<PipelineConfig>() {
                    @Override
                    public boolean test(PipelineConfig pipelineConfig) {
                        return pipelineConfig.name().toString().equalsIgnoreCase(pipelineWithIssue);
                    }
                }).forEach(new Consumer<PipelineConfig>() {
                    @Override
                    public void accept(PipelineConfig pipelineConfig) {
                        String pipelineNameAsSavedInConfig = pipelineConfig.name().toString();
                        pipelineSqlMapDao.deleteOldPipelineLabelCountForPipeline(pipelineNameAsSavedInConfig);
                    }
                });
            }
        });
    }
}
