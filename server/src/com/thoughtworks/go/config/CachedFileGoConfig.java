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

import com.thoughtworks.go.config.validation.GoConfigValidity;
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
 * @understands when to reload the config file
 */
@Component
public class CachedFileGoConfig implements CachedGoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileGoConfig.class);

    private final GoFileConfigDataSource dataSource;
    private final ServerHealthService serverHealthService;
    private List<ConfigChangedListener> listeners = new ArrayList<ConfigChangedListener>();

    private volatile CruiseConfig currentConfig;
    private volatile CruiseConfig currentConfigForEdit;
    private volatile Exception lastException;
    private volatile GoConfigHolder configHolder;

    @Autowired public CachedFileGoConfig(GoFileConfigDataSource dataSource, ServerHealthService serverHealthService) {
        this.dataSource = dataSource;
        this.serverHealthService = serverHealthService;
    }

    @Override
    public CruiseConfig loadForEditing() {
        loadConfigIfNull();
        return currentConfigForEdit;
    }

    @Override
    public CruiseConfig currentConfig() {
        if (currentConfig == null) {
            return new BasicCruiseConfig();
        }
        return currentConfig;
    }

    private void throwExceptionIfExists() {
        if (lastException != null) {
            throw bomb("Invalid config file", lastException);
        }
    }

    @Override
    public void loadConfigIfNull() {
        if (currentConfig == null || currentConfigForEdit == null || configHolder == null) {
            loadFromDisk();
        }
    }

    @Override
    public void forceReload() {
        loadFromDisk();
    }

    //NOTE: This method is called on a thread from Spring
    public void onTimer() {
        this.forceReload();
    }

    private void loadFromDisk() {
        try {
            LOGGER.debug("Config file (on disk) update check is in queue");
            synchronized (GoConfigWriteLock.class) {
                LOGGER.debug("Config file (on disk) update check is in progress");
                GoConfigHolder configHolder = dataSource.load();
                if (configHolder != null) {
                    saveValidConfigToCacheAndNotifyConfigChangeListeners(configHolder);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error loading cruise-config.xml from disk, keeping previous one", e);
            saveConfigError(e);
        }
    }

    static class PipelineConfigSaveResult {
        private PipelineConfig pipelineConfig;
        private String group;
        private GoConfigHolder configHolder;

        public PipelineConfigSaveResult(PipelineConfig pipelineConfig, String group, GoConfigHolder configHolder) {
            this.pipelineConfig = pipelineConfig;
            this.group = group;
            this.configHolder = configHolder;
        }

        public PipelineConfig getPipelineConfig() {
            return pipelineConfig;
        }

        public String getGroup() {
            return group;
        }

        public GoConfigHolder getConfigHolder() {
            return configHolder;
        }
    }

    @Override
    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand) {
        GoConfigHolder holder = new GoConfigHolder(currentConfig, currentConfigForEdit);
        return writeWithLock(updateConfigCommand, holder);
    }

    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand, GoConfigHolder holder) {
        GoFileConfigDataSource.GoConfigSaveResult saveResult = dataSource.writeWithLock(updateConfigCommand, holder);
        saveValidConfigToCacheAndNotifyConfigChangeListeners(saveResult.getConfigHolder());
        return saveResult.getConfigSaveState();
    }

    public synchronized void writePipelineWithLock(PipelineConfig pipelineConfig, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        GoConfigHolder serverCopy = new GoConfigHolder(currentConfig, currentConfigForEdit);
        writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
    }

    public synchronized PipelineConfigSaveResult writePipelineWithLock(PipelineConfig pipelineConfig, GoConfigHolder serverCopy, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        PipelineConfigSaveResult saveResult = dataSource.writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
        saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(saveResult);
        return saveResult;
    }

    private void saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(CachedFileGoConfig.PipelineConfigSaveResult saveResult) {
        saveValidConfigToCache(saveResult.getConfigHolder());
        LOGGER.info("About to notify pipeline config listeners");

        for (ConfigChangedListener listener : listeners) {
            if(listener instanceof PipelineConfigChangedListener){
                try {
                    long startTime = System.currentTimeMillis();
                    ((PipelineConfigChangedListener) listener).onPipelineConfigChange(saveResult.getPipelineConfig(), saveResult.getGroup());
                    LOGGER.debug("Notifying {} took (in ms): {}", listener.getClass(), (System.currentTimeMillis() - startTime));
                } catch (Exception e) {
                    LOGGER.error("failed to fire config changed event for listener: " + listener, e);
                }

            }
        }
        LOGGER.info("Finished notifying pipeline config listeners");
    }

    private synchronized void saveValidConfigToCacheAndNotifyConfigChangeListeners(GoConfigHolder configHolder) {
        saveValidConfigToCache(configHolder);
        if(configHolder!=null) {
            notifyListeners(currentConfig);
        }
    }

    private synchronized void saveValidConfigToCache(GoConfigHolder configHolder) {
        if (configHolder != null) {
            LOGGER.debug("[Config Save] Saving config to the cache");
            this.lastException = null;
            this.configHolder = configHolder;
            this.currentConfig = this.configHolder.config;
            this.currentConfigForEdit = this.configHolder.configForEdit;
            serverHealthService.update(ServerHealthState.success(invalidConfigType()));
        }
    }

    private synchronized void saveConfigError(Exception e) {
        this.lastException = e;
        ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_XML, GoConfigValidity.invalid(e).errorMessage(), invalidConfigType());
        serverHealthService.update(state);
    }

    private static HealthStateType invalidConfigType() {
        return HealthStateType.invalidConfig();
    }

    @Override
    public String getFileLocation() {
        return dataSource.fileLocation().getAbsolutePath();
    }

    @Override
    public synchronized void save(String configFileContent, boolean shouldMigrate) throws Exception {
        GoConfigHolder newConfigHolder = dataSource.write(configFileContent, shouldMigrate);
        saveValidConfigToCacheAndNotifyConfigChangeListeners(newConfigHolder);
    }

    @Override
    public GoConfigValidity checkConfigFileValid() {
        Exception ex = lastException;
        if (ex != null) {
            return GoConfigValidity.invalid(ex);
        }
        return GoConfigValidity.valid();
    }

    @Override
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
                long startTime = System.currentTimeMillis();
                listener.onConfigChange(newCruiseConfig);
                LOGGER.debug("Notifying {} took (in ms): {}", listener.getClass(), (System.currentTimeMillis() - startTime));
            } catch (Exception e) {
                LOGGER.error("Failed to fire config changed event for listener: " + listener, e);
            }
        }
        LOGGER.info("Finished notifying all listeners");
    }

    /**
     * @deprecated Used only in tests
     */
    @Override
    public synchronized void clearListeners() {
        listeners.clear();
    }

    /**
     * @deprecated Used only in tests
     */
    @Override
    public void reloadListeners() {
        notifyListeners(currentConfig());
    }

    @Override
    public GoConfigHolder loadConfigHolder() {
        return configHolder;
    }

    @Override
    public boolean hasListener(ConfigChangedListener listener) {
        return this.listeners.contains(listener);
    }
}
