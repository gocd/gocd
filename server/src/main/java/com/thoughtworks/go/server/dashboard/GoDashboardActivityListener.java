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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.domain.PipelineLockStatusChangeListener;
import com.thoughtworks.go.server.domain.PipelinePauseChangeListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor.Action;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineLockService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Listens to all activity that is needed to keep the dashboard updated and sets it up for processing.
 */
@Component
public class GoDashboardActivityListener implements Initializer, ConfigChangedListener, PipelinePauseChangeListener,
        PipelineLockStatusChangeListener {
    private final GoConfigService goConfigService;
    private final StageService stageService;
    private final PipelinePauseService pipelinePauseService;
    private final PipelineLockService pipelineLockService;
    private final GoDashboardStageStatusChangeHandler stageStatusChangeHandler;
    private final GoDashboardConfigChangeHandler configChangeHandler;
    private final GoDashboardPipelinePauseStatusChangeHandler pauseStatusChangeHandler;
    private final GoDashboardPipelineLockStatusChangeHandler lockStatusChangeHandler;
    private final GoDashboardTemplateConfigChangeHandler templateConfigChangeHandler;

    private final MultiplexingQueueProcessor processor;

    @Autowired
    public GoDashboardActivityListener(GoConfigService goConfigService,
                                       StageService stageService,
                                       PipelinePauseService pipelinePauseService,
                                       PipelineLockService pipelineLockService,
                                       GoDashboardStageStatusChangeHandler stageStatusChangeHandler,
                                       GoDashboardConfigChangeHandler configChangeHandler,
                                       GoDashboardPipelinePauseStatusChangeHandler pauseStatusChangeHandler,
                                       GoDashboardPipelineLockStatusChangeHandler lockStatusChangeHandler,
                                       GoDashboardTemplateConfigChangeHandler templateConfigChangeHandler) {
        this.goConfigService = goConfigService;
        this.stageService = stageService;
        this.pipelinePauseService = pipelinePauseService;
        this.pipelineLockService = pipelineLockService;

        this.stageStatusChangeHandler = stageStatusChangeHandler;
        this.configChangeHandler = configChangeHandler;
        this.pauseStatusChangeHandler = pauseStatusChangeHandler;
        this.lockStatusChangeHandler = lockStatusChangeHandler;
        this.templateConfigChangeHandler = templateConfigChangeHandler;

        this.processor = new MultiplexingQueueProcessor("Dashboard");
    }

    @Override
    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        goConfigService.register(securityConfigChangeListener());
        goConfigService.register(templateConfigChangedListener());
        goConfigService.register(pipelineGroupsChangedListener());
        stageService.addStageStatusListener(stageStatusChangedListener());
        pipelinePauseService.registerListener(this);
        pipelineLockService.registerListener(this);
    }

    @Override
    public void startDaemon() {
        processor.start();
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

    protected EntityConfigChangedListener<PipelineConfigs> pipelineGroupsChangedListener() {
        return new EntityConfigChangedListener<PipelineConfigs>() {
            @Override
            public void onEntityConfigChange(final PipelineConfigs pipelineConfigs) {
                processor.add(new Action() {
                    @Override
                    public void call() {
                        configChangeHandler.call(goConfigService.currentCruiseConfig());
                    }

                    @Override
                    public String description() {
                        return "pipeline configs: " + pipelineConfigs;
                    }
                });
            }
        };
    }

    protected EntityConfigChangedListener<PipelineTemplateConfig> templateConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineTemplateConfig>() {
            @Override
            public void onEntityConfigChange(final PipelineTemplateConfig templateConfig) {
                processor.add(new Action() {
                    @Override
                    public void call() {
                        templateConfigChangeHandler.call(templateConfig);
                    }

                    @Override
                    public String description() {
                        return "template config: " + templateConfig;
                    }
                });
            }
        };
    }

    private StageStatusListener stageStatusChangedListener() {
        return stage -> processor.add(new Action() {
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
    public void pauseStatusChanged(final PipelinePauseChangeListener.Event event) {
        processor.add(new Action() {
            @Override
            public void call() {
                pauseStatusChangeHandler.call(event);
            }

            @Override
            public String description() {
                return "pause event: " + event;
            }
        });
    }

    @Override
    public void lockStatusChanged(final PipelineLockStatusChangeListener.Event event) {
        processor.add(new Action() {
            @Override
            public void call() {
                lockStatusChangeHandler.call(event);
            }

            @Override
            public String description() {
                return "lock event: " + event;
            }
        });
    }
}
