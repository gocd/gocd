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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

/**
 * This class find the location of cruise-config.xml and turn that into stream
 * and passing it into MagicLoader or MagicWriter.
 */
@Component
public class GoFileConfigDataSource {
    private static final Logger LOGGER = Logger.getLogger(GoFileConfigDataSource.class);
    private final Charset UTF_8 = Charset.forName("UTF-8");
    private final CachedGoPartials cachedGoPartials;

    private ReloadStrategy reloadStrategy = new ReloadIfModified();
    private final MagicalGoConfigXmlWriter magicalGoConfigXmlWriter;
    private final MagicalGoConfigXmlLoader magicalGoConfigXmlLoader;
    private final ConfigRepository configRepository;
    private SystemEnvironment systemEnvironment;
    private GoConfigMigration upgrader;
    private final TimeProvider timeProvider;
    private ServerVersion serverVersion;
    private Cloner cloner = new Cloner();
    public static final String FILESYSTEM = "Filesystem";
    private ServerHealthService serverHealthService;

    /* Will only upgrade cruise config file on application startup. */
    @Autowired
    public GoFileConfigDataSource(GoConfigMigration upgrader, ConfigRepository configRepository, SystemEnvironment systemEnvironment, TimeProvider timeProvider, ConfigCache configCache,
                                  ServerVersion serverVersion, ConfigElementImplementationRegistry configElementImplementationRegistry,
                                  ServerHealthService serverHealthService, CachedGoPartials cachedGoPartials) {
        this(upgrader, configRepository, systemEnvironment, timeProvider, serverVersion,
                new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry),
                new MagicalGoConfigXmlWriter(configCache, configElementImplementationRegistry), serverHealthService, cachedGoPartials);
    }

    GoFileConfigDataSource(GoConfigMigration upgrader, ConfigRepository configRepository, SystemEnvironment systemEnvironment, TimeProvider timeProvider,
                           ServerVersion serverVersion, MagicalGoConfigXmlLoader magicalGoConfigXmlLoader, MagicalGoConfigXmlWriter magicalGoConfigXmlWriter,
                           ServerHealthService serverHealthService, CachedGoPartials cachedGoPartials) {
        this.configRepository = configRepository;
        this.systemEnvironment = systemEnvironment;
        this.upgrader = upgrader;
        this.timeProvider = timeProvider;
        this.serverVersion = serverVersion;
        this.magicalGoConfigXmlLoader = magicalGoConfigXmlLoader;
        this.magicalGoConfigXmlWriter = magicalGoConfigXmlWriter;
        this.serverHealthService = serverHealthService;
        this.cachedGoPartials = cachedGoPartials;
    }

    public GoFileConfigDataSource reloadEveryTime() {
        this.reloadStrategy = new AlwaysReload();
        return this;
    }

    public GoFileConfigDataSource reloadIfModified() {
        this.reloadStrategy = new ReloadIfModified();
        return this;
    }

    public File fileLocation() {
        return new File(systemEnvironment.getCruiseConfigFile());
    }

    public GoConfigHolder load() throws Exception {
        File configFile = fileLocation();

        ReloadStrategy.ReloadTestResult result = reloadStrategy.requiresReload(configFile);
        if (!result.requiresReload) {
            reloadStrategy.hasLatest(result);
            return null;
        }
        synchronized (this) {
            result = reloadStrategy.requiresReload(configFile);
            if (!result.requiresReload) {
                reloadStrategy.hasLatest(result);
                return null;
            }

            reloadStrategy.performingReload(result);

            LOGGER.info("Config file changed at " + result.modifiedTime);
            LOGGER.info("Reloading config file: " + configFile);

            encryptPasswords(configFile);
            LOGGER.debug("Detected change in config file.");
            return forceLoad(configFile);
        }
    }

    private void encryptPasswords(File configFile) throws Exception {
        String currentContent = FileUtils.readFileToString(configFile);
        GoConfigHolder configHolder = magicalGoConfigXmlLoader.loadConfigHolder(currentContent);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        magicalGoConfigXmlWriter.write(configHolder.configForEdit, stream, true);
        String postEncryptContent = new String(stream.toByteArray());
        if (!currentContent.equals(postEncryptContent)) {
            LOGGER.debug("[Encrypt] Writing config to file");
            FileUtils.writeStringToFile(configFile, postEncryptContent);
        }
    }

    synchronized GoConfigHolder forceLoad(File configFile) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reloading config file: " + configFile.getAbsolutePath());
        }

        try {
            return internalLoad(FileUtils.readFileToString(configFile), new ConfigModifyingUser(FILESYSTEM));
        } catch (Exception e) {
            LOGGER.error("Unable to load config file: " + configFile.getAbsolutePath() + " " + e.getMessage(), e);
            if (configFile.exists()) {
                LOGGER.warn("--- " + configFile.getAbsolutePath() + " ---");
                LOGGER.warn(FileUtil.readContentFromFile(configFile));
                LOGGER.warn("------");
            }
            LOGGER.debug(e);
            throw e;
        }
    }

    @Deprecated
    public synchronized GoConfigHolder write(String configFileContent, boolean shouldMigrate) throws Exception {
        File configFile = fileLocation();
        try {
            if (shouldMigrate) {
                configFileContent = upgrader.upgradeIfNecessary(configFileContent);
            }
            GoConfigHolder configHolder = internalLoad(configFileContent, new ConfigModifyingUser());
            String toWrite = configAsXml(configHolder.configForEdit, false);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Writing config file: " + configFile.getAbsolutePath());
            }
            writeToConfigXmlFile(toWrite);
            return configHolder;
        } catch (Exception e) {
            LOGGER.error("Unable to write config file: " + configFile.getAbsolutePath()
                    + "\n" + e.getMessage(), e);
            throw e;
        }
    }

    private void writeToConfigXmlFile(String content) {
        FileChannel channel = null;
        FileOutputStream outputStream = null;
        FileLock lock = null;
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileLocation(), "rw");
            channel = randomAccessFile.getChannel();
            lock = channel.lock();
            randomAccessFile.seek(0);
            randomAccessFile.setLength(0);
            outputStream = new FileOutputStream(randomAccessFile.getFD());

            IOUtils.write(content, outputStream, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null && lock != null) {
                try {
                    lock.release();
                    channel.close();
                    IOUtils.closeQuietly(outputStream);
                } catch (IOException e) {
                    LOGGER.error("Error occured when releasing file lock and closing file.", e);
                }
            }
        }
    }

    public synchronized EntityConfigSaveResult writeEntityWithLock(EntityConfigUpdateCommand updatingCommand, GoConfigHolder configHolder, Username currentUser) {
        CruiseConfig modifiedConfig = cloner.deepClone(configHolder.configForEdit);
        try {
            updatingCommand.update(modifiedConfig);
        } catch (Exception e) {
            bomb(e);
        }
        CruiseConfig preprocessedConfig = cloner.deepClone(modifiedConfig);
        MagicalGoConfigXmlLoader.preprocess(preprocessedConfig);

        if (updatingCommand.isValid(preprocessedConfig)) {
            try {
                LOGGER.info("[Configuration Changed] Saving updated configuration.");
                String configAsXml = configAsXml(modifiedConfig, true);
                String md5 = CachedDigestUtils.md5Hex(configAsXml);
                MagicalGoConfigXmlLoader.setMd5(modifiedConfig, md5);
                MagicalGoConfigXmlLoader.setMd5(preprocessedConfig, md5);
                writeToConfigXmlFile(configAsXml);
                configRepository.checkin(new GoConfigRevision(configAsXml, md5, currentUser.getUsername().toString(), serverVersion.version(), timeProvider));
                LOGGER.debug("[Config Save] Done writing with lock");
                reloadStrategy.latestState(preprocessedConfig);
                return new EntityConfigSaveResult(updatingCommand.getPreprocessedEntityConfig(), new GoConfigHolder(preprocessedConfig, modifiedConfig));
            } catch (Exception e) {
                throw new RuntimeException("failed to save : " + e.getMessage());
            }
        } else {
            throw new GoConfigInvalidException(preprocessedConfig, "Validation failed.");
        }
    }

    public synchronized GoConfigSaveResult writeWithLock(UpdateConfigCommand updatingCommand, GoConfigHolder configHolder) {
        try {

            // Need to convert to xml before we try to write it to the config file.
            // If our cruiseConfig fails XSD validation, we don't want to write it incorrectly.
            GoConfigHolder validatedConfigHolder;
            try {
                String configAsXml = trySavingConfig(updatingCommand, configHolder, cachedGoPartials.lastKnownPartials());
                validatedConfigHolder = internalLoad(configAsXml, getConfigUpdatingUser(updatingCommand));
                serverHealthService.update(ServerHealthState.success(HealthStateType.invalidConfigMerge()));
            } catch (GoConfigInvalidException e) {
                if (cachedGoPartials.lastValidPartials().isEmpty() && !(updatingCommand instanceof GoPartialConfig.GoPartialUpdateCommand)) {
                    throw e;
                } else {
                    serverHealthService.update(ServerHealthState.error(GoPartialConfig.INVALID_CRUISE_CONFIG_MERGE, GoConfigValidity.invalid(e).errorMessage(), HealthStateType.invalidConfigMerge()));
                    String configAsXml = trySavingConfig(updatingCommand, configHolder, cachedGoPartials.lastValidPartials());
                    validatedConfigHolder = internalLoad(configAsXml, getConfigUpdatingUser(updatingCommand));
                }
            }
            ConfigSaveState configSaveState = shouldMergeConfig(updatingCommand, configHolder) ? ConfigSaveState.MERGED : ConfigSaveState.UPDATED;
            return new GoConfigSaveResult(validatedConfigHolder, configSaveState);
        } catch (ConfigFileHasChangedException e) {
            LOGGER.warn("Configuration file could not be merged successfully after a concurrent edit: " + e.getMessage(), e);
            throw e;
        } catch (GoConfigInvalidException e) {
            LOGGER.warn("Configuration file is invalid: " + e.getMessage(), e);
            throw bomb(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Configuration file is not valid: " + e.getMessage(), e);
            throw bomb(e.getMessage(), e);
        } finally {
            LOGGER.debug("[Config Save] Done writing with lock");
        }
    }

    private String trySavingConfig(UpdateConfigCommand updatingCommand, GoConfigHolder configHolder, List<PartialConfig> partials) throws Exception {
        List<PartialConfig> clonedPartials = new Cloner().deepClone(partials);
        String configAsXml = getModifiedConfig(updatingCommand, configHolder, clonedPartials);
        LOGGER.info(String.format("[Configuration Changed] Saving updated configuration."));
        writeToConfigXmlFile(configAsXml);
        return configAsXml;
    }

    private ConfigModifyingUser getConfigUpdatingUser(UpdateConfigCommand updatingCommand) {
        return updatingCommand instanceof UserAware ? ((UserAware) updatingCommand).user() : new ConfigModifyingUser();
    }

    private String getModifiedConfig(UpdateConfigCommand updatingCommand, GoConfigHolder configHolder, List<PartialConfig> partials) throws Exception {
        LOGGER.debug("[Config Save] ==-- Getting modified config");
        if (shouldMergeConfig(updatingCommand, configHolder)) {
            if (!systemEnvironment.get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)) {
                throw new ConfigMergeException(ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH);
            }
            return getMergedConfig((NoOverwriteUpdateConfigCommand) updatingCommand, configHolder.configForEdit.getMd5(), partials);
        }
        CruiseConfig cruiseConfig = cloner.deepClone(configHolder.configForEdit);
        cruiseConfig.setPartials(partials);
        CruiseConfig config = updatingCommand.update(cruiseConfig);
        LOGGER.debug("[Config Save] ==-- Done getting modified config");
        String configAsXml = configAsXml(config, false);
        cachedGoPartials.markAsValid(cruiseConfig.getPartials());
        return configAsXml;
    }

    private boolean shouldMergeConfig(UpdateConfigCommand updatingCommand, GoConfigHolder configHolder) {
        LOGGER.debug("[Config Save] Checking whether config should be merged");
        if (updatingCommand instanceof NoOverwriteUpdateConfigCommand) {
            NoOverwriteUpdateConfigCommand noOverwriteCommand = (NoOverwriteUpdateConfigCommand) updatingCommand;
            if (!configHolder.configForEdit.getMd5().equals(noOverwriteCommand.unmodifiedMd5())) {
                return true;
            }
        }
        return false;
    }

    private String getMergedConfig(NoOverwriteUpdateConfigCommand noOverwriteCommand, String latestMd5, List<PartialConfig> partials) throws Exception {
        LOGGER.debug("[Config Save] Getting merged config");
        String oldMd5 = noOverwriteCommand.unmodifiedMd5();
        CruiseConfig modifiedConfig = getOldConfigAndMutateWithChanges(noOverwriteCommand, oldMd5);
        modifiedConfig.setPartials(partials);
        String modifiedConfigAsXml = convertMutatedConfigToXml(modifiedConfig, latestMd5);

        GoConfigRevision configRevision = new GoConfigRevision(modifiedConfigAsXml, "temporary-md5-for-branch", getConfigUpdatingUser(noOverwriteCommand).getUserName(),
                serverVersion.version(), timeProvider);

        String mergedConfigXml = configRepository.getConfigMergedWithLatestRevision(configRevision, oldMd5);
        validateMergedXML(mergedConfigXml, latestMd5);
        return mergedConfigXml;
    }

    private CruiseConfig validateMergedXML(String mergedConfigXml, String latestMd5) throws Exception {
        LOGGER.debug("[Config Save] -=- Converting merged config to XML");
        try {
            return magicalGoConfigXmlLoader.loadConfigHolder(mergedConfigXml).configForEdit;
        } catch (Exception e) {
            LOGGER.info(format("[CONFIG_MERGE] Post merge validation failed, latest-md5: %s", latestMd5));
            throw new ConfigMergePostValidationException(e.getMessage(), e);
        } finally {
            LOGGER.debug("[Config Save] -=- Done converting merged config to XML");
        }
    }

    private String convertMutatedConfigToXml(CruiseConfig modifiedConfig, String latestMd5) throws Exception {
        try {
            return configAsXml(modifiedConfig, false);
        } catch (Exception e) {
            LOGGER.info(format("[CONFIG_MERGE] Pre merge validation failed, latest-md5: %s", latestMd5));
            throw new ConfigMergePreValidationException(e.getMessage(), e);
        }
    }

    private CruiseConfig getOldConfigAndMutateWithChanges(NoOverwriteUpdateConfigCommand noOverwriteCommand, String oldMd5) throws Exception {
        LOGGER.debug("[Config Save] --- Mutating old config");
        String configXmlAtOldMd5 = configRepository.getRevision(oldMd5).getContent();
        CruiseConfig cruiseConfigAtOldMd5 = magicalGoConfigXmlLoader.fromXmlPartial(configXmlAtOldMd5, BasicCruiseConfig.class);
        CruiseConfig config = noOverwriteCommand.update(cruiseConfigAtOldMd5);
        LOGGER.debug("[Config Save] --- Done mutating old config");
        return config;
    }

    private GoConfigHolder internalLoad(final String content, final ConfigModifyingUser configModifyingUser) throws Exception {
        GoConfigHolder configHolder = magicalGoConfigXmlLoader.loadConfigHolder(content);
        CruiseConfig config = configHolder.config;
        reloadStrategy.latestState(config);
        configRepository.checkin(new GoConfigRevision(content, configHolder.configForEdit.getMd5(), configModifyingUser.getUserName(), serverVersion.version(), timeProvider));
        return configHolder;
    }

    public String configAsXml(CruiseConfig config, boolean skipPreprocessingAndValidation) throws Exception {
        LOGGER.debug("[Config Save] === Converting config to XML");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        magicalGoConfigXmlWriter.write(config, outputStream, skipPreprocessingAndValidation);
        LOGGER.debug("[Config Save] === Done converting config to XML");
        return outputStream.toString();
    }

    public void upgradeIfNecessary() {
        GoConfigMigrationResult migrationResult = this.upgrader.upgradeIfNecessary(fileLocation(), serverVersion.version());

        if (migrationResult.isUpgradeFailure()) {
            String message = migrationResult.message();
            serverHealthService.update(ServerHealthState.warning("Invalid Configuration", message, HealthStateType.general(HealthStateScope.forInvalidConfig())));
            LOGGER.warn(message);
        }
    }

    public String getFileLocation() {
        return fileLocation().getAbsolutePath();
    }

    static class GoConfigSaveResult {
        private final GoConfigHolder configHolder;
        private final ConfigSaveState configSaveState;

        GoConfigSaveResult(GoConfigHolder holder, ConfigSaveState configSaveState) {
            configHolder = holder;
            this.configSaveState = configSaveState;
        }

        public GoConfigHolder getConfigHolder() {
            return configHolder;
        }

        public ConfigSaveState getConfigSaveState() {
            return configSaveState;
        }
    }

    private interface ReloadStrategy {
        class ReloadTestResult {
            final boolean requiresReload;
            final long fileSize;
            final long modifiedTime;

            public ReloadTestResult(boolean requiresReload, long fileSize, long modifiedTime) {
                this.requiresReload = requiresReload;
                this.fileSize = fileSize;
                this.modifiedTime = modifiedTime;
            }
        }

        ReloadTestResult requiresReload(File configFile);

        void latestState(CruiseConfig config);

        void hasLatest(ReloadTestResult reloadTestResult);

        void performingReload(ReloadTestResult reloadTestResult);
    }

    private static class AlwaysReload implements ReloadStrategy {
        public ReloadTestResult requiresReload(File configFile) {
            return new ReloadTestResult(true, 0, 0);
        }

        public void latestState(CruiseConfig config) {
        }

        public void hasLatest(ReloadTestResult result) {
        }

        public void performingReload(ReloadTestResult result) {
        }
    }

    static class ReloadIfModified implements ReloadStrategy {
        private long lastModified;
        private long prevSize;
        private volatile String md5 = "";

        private long length(File configFile) {
            return configFile.length();
        }

        private long lastModified(File configFile) {
            return configFile.lastModified();
        }

        private boolean requiresReload(File configFile, long currentLastModified, long currentSize) {
            return doFileAttributesDiffer(currentLastModified, currentSize) && doesFileContentDiffer(configFile);
        }

        boolean doesFileContentDiffer(File configFile) {
            return !md5.equals(getConfigFileMd5(configFile));
        }

        boolean doFileAttributesDiffer(long currentLastModified, long currentSize) {
            return currentLastModified != lastModified ||
                    prevSize != currentSize;
        }

        public ReloadTestResult requiresReload(File configFile) {
            long lastModified = lastModified(configFile);
            long length = length(configFile);
            boolean requiresReload = requiresReload(configFile, lastModified, length);
            return new ReloadTestResult(requiresReload, length, lastModified);
        }

        private String getConfigFileMd5(File configFile) {
            String newMd5;
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                newMd5 = CachedDigestUtils.md5Hex(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return newMd5;
        }

        public void latestState(CruiseConfig config) {
            md5 = config.getMd5();
        }

        public void hasLatest(ReloadTestResult result) {
            rememberLatestFileAttributes(result);
        }

        private void rememberLatestFileAttributes(ReloadTestResult result) {
            synchronized (this) {
                lastModified = result.modifiedTime;
                prevSize = result.fileSize;
            }
        }

        public void performingReload(ReloadTestResult result) {
            rememberLatestFileAttributes(result);
        }
    }

}
