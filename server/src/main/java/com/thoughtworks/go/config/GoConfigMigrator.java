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

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;

/*
GoConfigMigrator is responsible to migrate the config xml to the latest version, this is called at server startup.
*/
@Component
public class GoConfigMigrator {
    private final GoConfigMigration goConfigMigration;
    private final SystemEnvironment systemEnvironment;
    private FullConfigSaveNormalFlow fullConfigSaveNormalFlow;
    private MagicalGoConfigXmlLoader loader;
    private GoConfigFileReader goConfigFileReader;
    private ConfigRepository configRepository;
    private ServerHealthService serverHealthService;
    private UpgradeFailedHandler upgradeFailedHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigMigrator.class.getName());

    @Autowired
    public GoConfigMigrator(GoConfigMigration goConfigMigration, SystemEnvironment systemEnvironment, ConfigCache configCache,
                            ConfigElementImplementationRegistry registry, FullConfigSaveNormalFlow fullConfigSaveNormalFlow,
                            ConfigRepository configRepository, ServerHealthService serverHealthService) {

        this(goConfigMigration, systemEnvironment, fullConfigSaveNormalFlow,
                new MagicalGoConfigXmlLoader(configCache, registry),
                new GoConfigFileReader(systemEnvironment), configRepository, serverHealthService,
                e -> {
                    e.printStackTrace();
                    System.err.println(
                            "There are errors in the Cruise config file.  Please read the error message and correct the errors.\n"
                                    + "Once fixed, please restart GoCD.\nError: " + e.getMessage());
                    LOGGER.error(MarkerFactory.getMarker("FATAL"),
                            "There are errors in the Cruise config file.  Please read the error message and correct the errors.\n"
                                    + "Once fixed, please restart GoCD.\nError: " + e.getMessage());
                    // Send exit signal in a separate thread otherwise it will deadlock jetty
                    new Thread(() -> System.exit(1)).start();
                });
    }

    public GoConfigMigrator(GoConfigMigration goConfigMigration, SystemEnvironment systemEnvironment,
                            FullConfigSaveNormalFlow fullConfigSaveNormalFlow, MagicalGoConfigXmlLoader loader,
                            GoConfigFileReader goConfigFileReader, ConfigRepository configRepository, ServerHealthService serverHealthService, UpgradeFailedHandler upgradeFailedHandler) {
        this.goConfigMigration = goConfigMigration;
        this.systemEnvironment = systemEnvironment;
        this.fullConfigSaveNormalFlow = fullConfigSaveNormalFlow;
        this.loader = loader;
        this.goConfigFileReader = goConfigFileReader;
        this.configRepository = configRepository;
        this.serverHealthService = serverHealthService;
        this.upgradeFailedHandler = upgradeFailedHandler;
    }

    public GoConfigHolder migrate() {
        try {
            return upgrade();
        } catch (Exception e) {
            upgradeFailedHandler.handle(e);
        }
        return null;
    }

    private GoConfigHolder upgrade() throws Exception {
        try {
            return upgradeConfigFile();
        } catch (Exception e) {
            LOGGER.warn("Error upgrading config file, trying to upgrade using the versioned config file.");
            return upgradeVersionedConfigFile(e);
        }
    }

    private GoConfigHolder upgradeConfigFile() throws Exception {
        String upgradedXml = this.goConfigMigration.upgradeIfNecessary(this.goConfigFileReader.configXml());

        LOGGER.info("[Config Save] Starting Config Save post upgrade using FullConfigSaveNormalFlow");

        CruiseConfig cruiseConfig = this.loader.deserializeConfig(upgradedXml);

        return fullConfigSaveNormalFlow.execute(new FullConfigUpdateCommand(cruiseConfig, null), new ArrayList<>(), "Upgrade");
    }

    private GoConfigHolder upgradeVersionedConfigFile(Exception originalException) throws Exception {
        GoConfigRevision currentConfigRevision = configRepository.getCurrentRevision();
        if (currentConfigRevision == null) {
            LOGGER.warn("There is no versioned configuration to fallback for migration.");
            throw originalException;
        }

        try {
            File backupFile = this.goConfigMigration.revertFileToVersion(fileLocation(), currentConfigRevision);
            logException(backupFile.getAbsolutePath(), originalException.getMessage());

            return upgradeConfigFile();
        } catch (Exception e) {
            LOGGER.warn("The versioned config file could be invalid or migrating the versioned config resulted in an invalid configuration");
            throw e;
        }
    }

    private void logException(String backupFileLocation, String exceptionMessage) {
        String invalidConfigMessage = String.format("Go encountered an invalid configuration file while starting up. "
                + "The invalid configuration file has been renamed to ‘%s’ and a new configuration file has been automatically " +
                "created using the last good configuration. Cause: '%s'", backupFileLocation, exceptionMessage);

        serverHealthService.update(ServerHealthState.warning("Invalid Configuration", invalidConfigMessage, HealthStateType.general(HealthStateScope.forInvalidConfig())));
        LOGGER.warn(invalidConfigMessage);
    }

    public File fileLocation() {
        return new File(systemEnvironment.getCruiseConfigFile());
    }

    public static interface UpgradeFailedHandler {
        void handle(Exception e);
    }
}
