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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.CheckedUpdateCommand;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UiBasedConfigUpdateCommand implements NoOverwriteUpdateConfigCommand, CheckedUpdateCommand, ConfigAwareUpdate {
    private final String md5;
    private final UpdateConfigFromUI command;
    private final LocalizedOperationResult result;
    private CruiseConfig configAfter;
    private CruiseConfig cruiseConfig;

    public UiBasedConfigUpdateCommand(String md5, UpdateConfigFromUI command, LocalizedOperationResult result) {
        this.md5 = md5;
        this.command = command;
        this.result = result;
    }

    @Override
    public String unmodifiedMd5() {
        return md5;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) {
        this.cruiseConfig = cruiseConfig;
        Validatable node = command.node(this.cruiseConfig);
        command.update(node);
        return this.cruiseConfig;
    }

    @Override
    public void afterUpdate(CruiseConfig cruiseConfig) {
        configAfter = cruiseConfig;
    }

    @Override
    public CruiseConfig configAfter() {
        return configAfter;
    }

    public CruiseConfig cruiseConfig() {
        return cruiseConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        command.checkPermission(cruiseConfig, result);
        return result.isSuccessful();
    }
}
