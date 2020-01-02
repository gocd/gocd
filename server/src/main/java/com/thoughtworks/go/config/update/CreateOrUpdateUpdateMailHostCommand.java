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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;

public class CreateOrUpdateUpdateMailHostCommand implements EntityConfigUpdateCommand<MailHost> {

    private final MailHost newMailHost;
    private MailHost preprocessedEntityConfig;

    public CreateOrUpdateUpdateMailHostCommand(MailHost newMailHost) {
        this.newMailHost = newMailHost;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.server().setMailHost(newMailHost);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedEntityConfig = preprocessedConfig.server().mailHost();

        preprocessedEntityConfig.validate(new ConfigSaveValidationContext(preprocessedConfig));

        if (preprocessedEntityConfig.errors().present()) {
            BasicCruiseConfig.copyErrors(preprocessedEntityConfig, newMailHost);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(newMailHost);
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
