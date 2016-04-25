/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.PipelineConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.server.service.GoConfigService.INVALID_CRUISE_CONFIG_XML;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands when to reload the config file or other config source
 */
@Component
public class MergedGoConfig implements CachedGoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileGoConfig.class);
    private CachedFileGoConfig fileService;
    private final ServerHealthService serverHealthService;
    private List<ConfigChangedListener> listeners = new ArrayList<>();
    private volatile CruiseConfig currentConfig;
    private volatile CruiseConfig currentConfigForEdit;
    private volatile GoConfigHolder configHolder;
    private volatile Exception lastException;

    @Autowired
    public MergedGoConfig(ServerHealthService serverHealthService,
                          CachedFileGoConfig fileService) {
        this.serverHealthService = serverHealthService;
        this.fileService = fileService;
    }

    public static List<ConfigErrors> validate(CruiseConfig config) {
        List<ConfigErrors> validationErrors = new ArrayList<ConfigErrors>();
        validationErrors.addAll(config.validateAfterPreprocess());
        return validationErrors;
    }

    //used in tests
    public void throwExceptionIfExists() {
        if (lastException != null) {
            throw bomb("Invalid config file", lastException);
        }
    }

    //NOTE: This method is called on a thread from Spring
    public void onTimer() {
        this.forceReload();
    }

    public void forceReload() {
        LOGGER.debug("Config file (on disk) update check is in queue");
        synchronized (GoConfigWriteLock.class) {
            LOGGER.debug("Config file (on disk) update check is in progress");
            try {
                GoConfigHolder configHolder = this.fileService.loadFromDisk();
                if (configHolder != null) {
                    saveValidConfigToCacheAndNotifyConfigChangeListeners(configHolder);
                }
            } catch (Exception e) {
                LOGGER.warn("Error loading cruise-config.xml from disk, keeping previous one", e);
                saveConfigError(e);
            }
        }
    }

    private synchronized void saveConfigError(Exception e) {
        this.lastException = e;
        ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_XML, GoConfigValidity.invalid(e).errorMessage(), HealthStateType.invalidConfig());
        serverHealthService.update(state);
    }

    public CruiseConfig loadForEditing() {
        return currentConfigForEdit;
    }

    public CruiseConfig currentConfig() {
        if (currentConfig == null) {
            currentConfig = new BasicCruiseConfig();
        }
        return currentConfig;
    }

    public void loadConfigIfNull() {
        if (currentConfig == null || currentConfigForEdit == null || configHolder == null) {
            forceReload();
        }
    }

    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand) {
        GoFileConfigDataSource.GoConfigSaveResult saveResult = this.fileService.writeWithLock(updateConfigCommand, this.configHolder);
        saveValidConfigToCacheAndNotifyConfigChangeListeners(saveResult.getConfigHolder());
        return saveResult.getConfigSaveState();
    }

    @Override
    public synchronized void writePipelineWithLock(PipelineConfig pipelineConfig, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        CachedFileGoConfig.PipelineConfigSaveResult saveResult = fileService.writePipelineWithLock(pipelineConfig, this.configHolder, saveCommand, currentUser);
        saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(saveResult);
    }

    private void saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(CachedFileGoConfig.PipelineConfigSaveResult saveResult) {
        saveValidConfigToCache(saveResult.getConfigHolder());
        LOGGER.info("About to notify pipeline config listeners");

        for (ConfigChangedListener listener : listeners) {
            if (listener instanceof PipelineConfigChangedListener) {
                try {
                    long startTime = System.currentTimeMillis();
                    ((PipelineConfigChangedListener) listener).onPipelineConfigChange(saveResult.getPipelineConfig(), saveResult.getGroup());
                    LOGGER.debug("Notifying {} took (in ms): {}", listener.getClass(), (System.currentTimeMillis() - startTime));
                } catch (Exception e) {
                    LOGGER.error("Failed to fire config changed event for listener: " + listener, e);
                }

            }
        }
        LOGGER.info("Finished notifying pipeline config listeners");
    }

    private synchronized void saveValidConfigToCache(GoConfigHolder configHolder) {
        if (configHolder != null) {
            LOGGER.debug("[Config Save] Saving config to the cache");
            this.lastException = null;
            this.configHolder = configHolder;
            this.currentConfig = this.configHolder.config;
            this.currentConfigForEdit = this.configHolder.configForEdit;
            serverHealthService.update(ServerHealthState.success(HealthStateType.invalidConfig()));
        }
    }

    private synchronized void saveValidConfigToCacheAndNotifyConfigChangeListeners(GoConfigHolder configHolder) {
        saveValidConfigToCache(configHolder);
        if (configHolder != null) {
            notifyListeners(currentConfig);
        }
    }

    public String getFileLocation() {
        return this.fileService.getFileLocation();
    }

    public void save(String configFileContent, boolean shouldMigrate) throws Exception {
        GoConfigHolder newConfigHolder = this.fileService.save(configFileContent, shouldMigrate);
        saveValidConfigToCacheAndNotifyConfigChangeListeners(newConfigHolder);
    }

    public GoConfigValidity checkConfigFileValid() {
        Exception ex = lastException;
        if (ex != null) {
            return GoConfigValidity.invalid(ex);
        }
        return GoConfigValidity.valid();
    }

    public synchronized void registerListener(ConfigChangedListener listener) {
        this.listeners.add(listener);
        if (currentConfig != null) {
            listener.onConfigChange(currentConfig);
        }
    }

    private synchronized void notifyListeners(CruiseConfig newCruiseConfig) {
        LOGGER.info("About to notify config listeners");
        for (ConfigChangedListener listener : listeners) {
            try {
                listener.onConfigChange(newCruiseConfig);
            } catch (Exception e) {
                LOGGER.error("Failed to fire config changed event for listener: " + listener, e);
            }
        }
        LOGGER.info("Finished notifying all listeners");
    }

    /**
     * @deprecated Used only in tests
     */
    public synchronized void clearListeners() {
        listeners.clear();
    }

    /**
     * @deprecated Used only in tests
     */
    public void reloadListeners() {
        notifyListeners(currentConfig());
    }

    public GoConfigHolder loadConfigHolder() {
        return configHolder;
    }

    @Override
    public boolean hasListener(ConfigChangedListener listener) {
        return this.listeners.contains(listener);
    }
}
