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
package com.thoughtworks.go.config;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.exceptions.ConfigMergePostValidationException;
import com.thoughtworks.go.config.exceptions.ConfigMergePreValidationException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/*
Given a CruiseConfig object, FullConfigSaveMergeFlow updates the config by merging with existing config in repo and by orchestrating the required steps from preprocessing,
validating to writing the config in appropriate locations.
*/

@Component
public class FullConfigSaveMergeFlow extends FullConfigSaveFlow{
    @Autowired
    public FullConfigSaveMergeFlow(ConfigCache configCache, ConfigElementImplementationRegistry configElementImplementationRegistry,
                                   SystemEnvironment systemEnvironment, TimeProvider timeProvider,
                                   ConfigRepository configRepository, CachedGoPartials cachedGoPartials) {
        this(new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry),
                new MagicalGoConfigXmlWriter(configCache, configElementImplementationRegistry), configElementImplementationRegistry,
                timeProvider, configRepository, cachedGoPartials, new GoConfigFileWriter(systemEnvironment));
    }

    FullConfigSaveMergeFlow(MagicalGoConfigXmlLoader loader, MagicalGoConfigXmlWriter writer,
                            ConfigElementImplementationRegistry configElementImplementationRegistry,
                            TimeProvider timeProvider, ConfigRepository configRepository,
                            CachedGoPartials cachedGoPartials, GoConfigFileWriter fileWriter) {
        super(loader, writer, configElementImplementationRegistry, timeProvider, configRepository, cachedGoPartials, fileWriter);
    }

    @Override
    public GoConfigHolder execute(FullConfigUpdateCommand updatingCommand, final List<PartialConfig> partials, String currentUser) throws Exception {
        LOGGER.debug("[Config Save] Starting Config Save using FullConfigSaveMergeFlow");

        CruiseConfig configForEdit = configForEditWithPartials(updatingCommand, partials);

        String configForEditXml = toXmlString(configForEdit, updatingCommand.unmodifiedMd5());

        String mergedConfig = getMergedConfig(configForEditXml, currentUser, updatingCommand.unmodifiedMd5());

        GoConfigHolder goConfigHolder = reloadConfig(mergedConfig, partials);

        checkinToConfigRepo(currentUser, goConfigHolder.configForEdit, mergedConfig);

        writeToConfigXml(mergedConfig);

        cachedGoPartials.markAsValid(partials);

        setMergedConfigForEditOn(goConfigHolder, partials);

        LOGGER.debug("[Config Save] Done Config Save using FullConfigSaveMergeFlow");

        return goConfigHolder;
    }

    private GoConfigHolder reloadConfig(String configXml, final List<PartialConfig> partials) {
        try {
            return loader.loadConfigHolder(configXml, cruiseConfig -> cruiseConfig.setPartials(partials));
        } catch (Exception e) {
            LOGGER.debug("[CONFIG_MERGE] Post merge validation failed");
            throw new ConfigMergePostValidationException(e.getMessage(), e);
        }
    }

    private String getMergedConfig(String modifiedConfigAsXml, String currentUser, String oldMd5) throws Exception {
        GoConfigRevision configRevision = new GoConfigRevision(modifiedConfigAsXml, "temporary-md5-for-branch", currentUser,
                CurrentGoCDVersion.getInstance().formatted(), timeProvider);

        return configRepository.getConfigMergedWithLatestRevision(configRevision, oldMd5);
    }

    private String toXmlString(CruiseConfig configForEdit, String md5) {
        LOGGER.debug("[CONFIG_MERGE] Validating and serializing CruiseConfig to xml before merge: Starting");
        String configForEditXml;

        try {
            preprocessAndValidate(configForEdit);
            configForEditXml = toXmlString(configForEdit);
        } catch (Exception e) {
            LOGGER.debug("[CONFIG_MERGE] Pre merge validation failed, latest-md5: {}", md5);
            throw new ConfigMergePreValidationException(e.getMessage(), e);
        }
        LOGGER.debug("[CONFIG_MERGE] Validating and serializing CruiseConfig to xml before merge: Done");

        return configForEditXml;
    }
}
