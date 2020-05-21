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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.stream.IntStream.range;

@Component
public class ConfigMaterialUpdateListenerFactory {
    private final ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue;
    private final GoConfigRepoConfigDataSource repoConfigDataSource;
    private final MaterialRepository materialRepository;
    private final MaterialUpdateCompletedTopic materialUpdateCompletedTopic;
    private final MaterialService materialService;
    private final SubprocessExecutionContext subprocessExecutionContext;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public ConfigMaterialUpdateListenerFactory(SystemEnvironment systemEnvironment,
                                               ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue,
                                               GoConfigRepoConfigDataSource repoConfigDataSource,
                                               MaterialRepository materialRepository,
                                               MaterialUpdateCompletedTopic materialUpdateCompletedTopic,
                                               MaterialService materialService,
                                               SubprocessExecutionContext subprocessExecutionContext) {
        this.systemEnvironment = systemEnvironment;
        this.configMaterialPostUpdateQueue = configMaterialPostUpdateQueue;
        this.repoConfigDataSource = repoConfigDataSource;
        this.materialRepository = materialRepository;
        this.materialUpdateCompletedTopic = materialUpdateCompletedTopic;
        this.materialService = materialService;
        this.subprocessExecutionContext = subprocessExecutionContext;
    }

    public void init() {
        int numberOfConfigMaterialPostUpdateListeners = systemEnvironment.getNumberOfConfigMaterialPostUpdateListeners();

        range(0, numberOfConfigMaterialPostUpdateListeners).forEach(i ->
                this.configMaterialPostUpdateQueue.addListener(new ConfigMaterialUpdateListener(repoConfigDataSource, materialRepository,
                        materialUpdateCompletedTopic, materialService, subprocessExecutionContext))
        );
    }
}
