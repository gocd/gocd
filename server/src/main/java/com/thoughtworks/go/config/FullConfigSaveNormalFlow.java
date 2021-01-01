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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/*
Given a CruiseConfig object, FullConfigSaveNormalFlow updates the config by orchestrating the required steps from preprocessing,
validating to writing the config in appropriate locations.
*/
@Component
public class FullConfigSaveNormalFlow extends FullConfigSaveFlow {

    @Autowired
    public FullConfigSaveNormalFlow(ConfigCache configCache, ConfigElementImplementationRegistry configElementImplementationRegistry,
                                    SystemEnvironment systemEnvironment, TimeProvider timeProvider,
                                    ConfigRepository configRepository, CachedGoPartials cachedGoPartials) {
        this(new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry),
                new MagicalGoConfigXmlWriter(configCache, configElementImplementationRegistry), configElementImplementationRegistry,
                timeProvider, configRepository, cachedGoPartials, new GoConfigFileWriter(systemEnvironment));
    }

    public FullConfigSaveNormalFlow(MagicalGoConfigXmlLoader loader, MagicalGoConfigXmlWriter writer,
                                    ConfigElementImplementationRegistry configElementImplementationRegistry,
                                    TimeProvider timeProvider,
                                    ConfigRepository configRepository, CachedGoPartials cachedGoPartials,
                                    GoConfigFileWriter fileWriter) {
        super(loader, writer, configElementImplementationRegistry, timeProvider, configRepository, cachedGoPartials, fileWriter);
    }

    @Override
    public GoConfigHolder execute(FullConfigUpdateCommand updatingCommand, List<PartialConfig> partials, String currentUser) throws Exception {
        LOGGER.debug("[Config Save] Starting Config Save using FullConfigSaveNormalFlow");

        CruiseConfig configForEdit = configForEditWithPartials(updatingCommand, partials);

        CruiseConfig preProcessedConfig = preprocessAndValidate(configForEdit);

        String configForEditXmlString = toXmlString(configForEdit);

        postValidationUpdates(configForEdit, configForEditXmlString);

        MagicalGoConfigXmlLoader.setMd5(preProcessedConfig, configForEdit.getMd5());

        checkinToConfigRepo(currentUser, configForEdit, configForEditXmlString);

        writeToConfigXml(configForEditXmlString);

        GoConfigHolder goConfigHolder = new GoConfigHolder(preProcessedConfig, configForEdit);

        setMergedConfigForEditOn(goConfigHolder, partials);

        cachedGoPartials.markAsValid(partials);

        LOGGER.debug("[Config Save] Done Config Save using FullConfigSaveNormalFlow");

        return goConfigHolder;
    }
}
