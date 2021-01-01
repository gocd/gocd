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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor.Action;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Listens to all activity that is needed to keep CCTray updated and sets it up for processing.
 */
@Component
public class CcTrayActivityListener implements Initializer, JobStatusListener, StageStatusListener, ConfigChangedListener {
    private static Logger LOGGER = LoggerFactory.getLogger(CcTrayActivityListener.class);
    private final GoConfigService goConfigService;
    private final CcTrayJobStatusChangeHandler jobStatusChangeHandler;
    private final CcTrayStageStatusChangeHandler stageStatusChangeHandler;
    private final CcTrayConfigChangeHandler configChangeHandler;

    private final MultiplexingQueueProcessor processor;

    @Autowired
    public CcTrayActivityListener(GoConfigService goConfigService, CcTrayJobStatusChangeHandler jobStatusChangeHandler,
                                  CcTrayStageStatusChangeHandler stageStatusChangeHandler,
                                  CcTrayConfigChangeHandler configChangeHandler) {
        this.goConfigService = goConfigService;
        this.jobStatusChangeHandler = jobStatusChangeHandler;
        this.stageStatusChangeHandler = stageStatusChangeHandler;
        this.configChangeHandler = configChangeHandler;

        this.processor = new MultiplexingQueueProcessor("CCTray");
    }

    @Override
    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        goConfigService.register(securityConfigChangeListener());
    }

    @Override
    public void startDaemon() {
        processor.start();
    }

    protected SecurityConfigChangeListener securityConfigChangeListener() {
        return new SecurityConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {
                processor.add(new Action() {
                    @Override
                    public void call() {
                        configChangeHandler.call(goConfigService.currentCruiseConfig());
                    }

                    @Override
                    public String description() {
                        return "security_config changed";
                    }
                });
            }
        };

    }

    @Override
    public void jobStatusChanged(final JobInstance job) {
        LOGGER.debug("Adding CCTray activity for job into queue: {}", job);
        processor.add(new Action() {
            @Override
            public void call() {
                LOGGER.debug("Handling CCTray activity for job: {}", job);

                jobStatusChangeHandler.call(job);
            }

            @Override
            public String description() {
                return "job: " + job;
            }
        });
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        LOGGER.debug("Adding CCTray activity for stage into queue: {}", stage);
        processor.add(new Action() {
            @Override
            public void call() {
                LOGGER.debug("Handling CCTray activity for stage: {}", stage);

                stageStatusChangeHandler.call(stage);
            }

            @Override
            public String description() {
                return "stage: " + stage;
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
}
