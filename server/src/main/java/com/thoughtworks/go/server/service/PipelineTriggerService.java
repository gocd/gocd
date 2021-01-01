/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.builders.ScheduleOptionsBuilder;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PipelineTriggerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineTriggerService.class);
    private BuildCauseProducerService buildCauseProducerService;
    private GoConfigService goConfigService;

    @Autowired
    public PipelineTriggerService(BuildCauseProducerService buildCauseProducerService, GoConfigService goConfigService) {
        this.buildCauseProducerService = buildCauseProducerService;
        this.goConfigService = goConfigService;
    }

    public void schedule(String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, Username username, final HttpOperationResult result) {
        LOGGER.info("[Pipeline Schedule] [Requested] Manual trigger of pipeline '{}' requested by {}", pipelineName, CaseInsensitiveString.str(username.getUsername()));
        ScheduleOptions scheduleOptions = new ScheduleOptionsBuilder(goConfigService).build(result, pipelineName, pipelineScheduleOptions);
        if (result.canContinue()) {
            LOGGER.info("[Pipeline Schedule] [Accepted] Manual trigger of pipeline '{}' accepted for user {}", pipelineName, CaseInsensitiveString.str(username.getUsername()));
            buildCauseProducerService.manualSchedulePipeline(username, new CaseInsensitiveString(pipelineName), scheduleOptions, result);
            LOGGER.info("[Pipeline Schedule] [Processed] Manual trigger of pipeline '{}' processed with result '{}'", pipelineName, result.getServerHealthState());
        }
    }
}
