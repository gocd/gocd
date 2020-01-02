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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;

public class DeleteMailHostCommand implements EntityConfigUpdateCommand<MailHost> {

    private MailHost preprocessedEntityConfig;

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedEntityConfig = preprocessedConfig.server().mailHost();
        preprocessedConfig.server().setMailHost(null);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }

    @Override
    public void clearErrors() {

    }

    @Override
    public MailHost getPreprocessedEntityConfig() {
        return preprocessedEntityConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return true;
    }

}
