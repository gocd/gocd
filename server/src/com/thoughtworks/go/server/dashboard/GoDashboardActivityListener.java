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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor.Action;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Listens to all activity that is needed to keep the dashboard updated and sets it up for processing.
 */
@Component
public class GoDashboardActivityListener implements Initializer, JobStatusListener, ConfigChangedListener {
    private final GoConfigService goConfigService;
    private final StageService stageService;
    private final GoDashboardJobStatusChangeHandler jobStatusChangeHandler;
    private final GoDashboardStageStatusChangeHandler stageStatusChangeHandler;
    private final GoDashboardConfigChangeHandler configChangeHandler;

    private final MultiplexingQueueProcessor processor;

    @Autowired
    public GoDashboardActivityListener(GoConfigService goConfigService, StageService stageService, GoDashboardJobStatusChangeHandler jobStatusChangeHandler,
                                       GoDashboardStageStatusChangeHandler stageStatusChangeHandler, GoDashboardConfigChangeHandler configChangeHandler) {
        this.goConfigService = goConfigService;
        this.stageService = stageService;
        this.jobStatusChangeHandler = jobStatusChangeHandler;
        this.stageStatusChangeHandler = stageStatusChangeHandler;
        this.configChangeHandler = configChangeHandler;

        this.processor = new MultiplexingQueueProcessor("Dashboard");
    }

    @Override
    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        stageService.addStageStatusListener(stageStatusChangedListener());
        processor.start();
    }

    @Override
    public void jobStatusChanged(final JobInstance job) {
        processor.add(new Action() {
            @Override
            public void call() {
                jobStatusChangeHandler.call(job);
            }

            @Override
            public String description() {
                return "job: " + job;
            }
        });
    }

    @Override
    public void onConfigChange(final CruiseConfig newConfig) {
        processor.add(new Action() {
            @Override
            public void call() {
                configChangeHandler.call(newConfig);
            }

            @Override
            public String description() {
                return "config change";
            }
        });
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(final PipelineConfig pipelineConfig) {
                processor.add(new Action() {
                    @Override
                    public void call() {
                        configChangeHandler.call(pipelineConfig);
                    }

                    @Override
                    public String description() {
                        return "pipeline config: " + pipelineConfig;
                    }
                });
            }
        };
    }

    private StageStatusListener stageStatusChangedListener() {
        return new StageStatusListener() {
            @Override
            public void stageStatusChanged(final Stage stage) {
                processor.add(new Action() {
                    @Override
                    public void call() {
                        stageStatusChangeHandler.call(stage);
                    }

                    @Override
                    public String description() {
                        return "stage: " + stage;
                    }
                });
            }
        };
    }
}
