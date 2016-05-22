/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;

public interface CachedGoConfig {

    CruiseConfig loadForEditing();

    CruiseConfig currentConfig();

    void forceReload();

    void loadConfigIfNull();

    ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand);

    void writePipelineWithLock(PipelineConfig pipelineConfig, PipelineConfigService.SaveCommand saveCommand, Username currentUser);

    String getFileLocation();

    void save(String configFileContent, boolean shouldMigrate) throws Exception;

    GoConfigValidity checkConfigFileValid();

    void registerListener(ConfigChangedListener listener);

    void clearListeners();

    void reloadListeners();

    GoConfigHolder loadConfigHolder();

    boolean hasListener(ConfigChangedListener listener);
}
