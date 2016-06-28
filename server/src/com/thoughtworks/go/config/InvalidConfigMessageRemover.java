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

package com.thoughtworks.go.config;

import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands removing server health message on valid config changes
 */
@Component
public class InvalidConfigMessageRemover implements ConfigChangedListener {
    private final GoConfigService goConfigService;
    private final ServerHealthService serverHealthService;
    private boolean registering;

    @Autowired
    public InvalidConfigMessageRemover(GoConfigService goConfigService, ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.serverHealthService = serverHealthService;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        if (registering) {
            registering = false;
            return;
        }
        serverHealthService.removeByScope(HealthStateScope.forInvalidConfig());
    }

    public void initialize() {
        registering = true;
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final InvalidConfigMessageRemover invalidConfigMessageRemover = this;
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig entity) {
                invalidConfigMessageRemover.onConfigChange(null);
            }
        };
    }
}
