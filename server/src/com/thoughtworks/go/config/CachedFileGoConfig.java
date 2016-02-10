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
    private volatile Exception lastException;

    @Autowired public CachedFileGoConfig(GoFileConfigDataSource dataSource, ServerHealthService serverHealthService) {
        this.dataSource = dataSource;
        this.serverHealthService = serverHealthService;
    }

    @Override
    public CruiseConfig loadForEditing() {
        throw new RuntimeException("shouldn't get called");
//        loadConfigIfNull();
//        return currentConfigForEdit;
    }

    @Override
    public CruiseConfig currentConfig() {
        throw new RuntimeException("shouldn't get called");

//        if (currentConfig == null) {
//            return new BasicCruiseConfig();
//        }
//        return currentConfig;
    }

    private void throwExceptionIfExists() {
        if (lastException != null) {
            throw bomb("Invalid config file", lastException);
        }
    }

    @Override
    public void loadConfigIfNull() {
        throw new RuntimeException("shouldn't get called");

//        if (currentConfig == null || currentConfigForEdit == null || configHolder == null) {
//            loadFromDisk();
//        }
    }

    @Override
    public void forceReload() {
        throw new RuntimeException("shouldn't get called");
//        loadFromDisk();
    }

//    //NOTE: This method is called on a thread from Spring
//    public void onTimer() {
//        throw new RuntimeException("shouldn't get called");
////        this.forceReload();
//    }

    public GoConfigHolder loadFromDisk() {
        try {
            return dataSource.load();
        } catch (Exception e) {
            LOGGER.warn("Error loading cruise-config.xml from disk, keeping previous one", e);
            saveConfigError(e);
            return null;
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

    //TODO: jyoti, remove this
    @Deprecated
    @Override
    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand) {
        throw new RuntimeException("shouldn't get called");

//        GoConfigHolder holder = new GoConfigHolder(currentConfig, currentConfigForEdit);
//        return writeWithLock(updateConfigCommand, holder);
    }

    //TODO: jyoti, remove this
    @Deprecated
    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand, GoConfigHolder holder) {
        throw new RuntimeException("shouldn't get called");
//        GoFileConfigDataSource.GoConfigSaveResult saveResult = dataSource.writeWithLock(updateConfigCommand, holder);
////        saveValidConfigToCacheAndNotifyConfigChangeListeners(saveResult.getConfigHolder());
//        return saveResult.getConfigSaveState();
    }

    public synchronized GoFileConfigDataSource.GoConfigSaveResult writeWithLockNew(UpdateConfigCommand updateConfigCommand, GoConfigHolder holder) {
        return dataSource.writeWithLock(updateConfigCommand, holder);
    }

    //TODO: jyoti, remove this
    @Deprecated
    public synchronized void writePipelineWithLock(PipelineConfig pipelineConfig, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        throw new RuntimeException("shouldn't get called");
//        GoConfigHolder serverCopy = new GoConfigHolder(currentConfig, currentConfigForEdit);
//        writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
    }

    //TODO: jyoti, remove this
    @Deprecated
    public synchronized PipelineConfigSaveResult writePipelineWithLock(PipelineConfig pipelineConfig, GoConfigHolder serverCopy, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        throw new RuntimeException("shouldn't get called");
//        PipelineConfigSaveResult saveResult = dataSource.writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
////        saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(saveResult);
//        return saveResult;
    }
    public synchronized PipelineConfigSaveResult writePipelineWithLockNew(PipelineConfig pipelineConfig, GoConfigHolder serverCopy, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        return dataSource.writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
    }

//    private void saveValidConfigToCacheAndNotifyPipelineConfigChangeListeners(CachedFileGoConfig.PipelineConfigSaveResult saveResult) {
//        saveValidConfigToCache(saveResult.getConfigHolder());
//        LOGGER.info("About to notify pipeline config listeners");
//
//        for (ConfigChangedListener listener : listeners) {
//            if(listener instanceof PipelineConfigChangedListener){
//                try {
//                    long startTime = System.currentTimeMillis();
//                    ((PipelineConfigChangedListener) listener).onPipelineConfigChange(saveResult.getPipelineConfig(), saveResult.getGroup());
//                    LOGGER.debug("Notifying {} took (in ms): {}", listener.getClass(), (System.currentTimeMillis() - startTime));
//                } catch (Exception e) {
//                    LOGGER.error("failed to fire config changed event for listener: " + listener, e);
//                }
//
//            }
//        }
//        LOGGER.info("Finished notifying pipeline config listeners");
//    }

//    private synchronized void saveValidConfigToCacheAndNotifyConfigChangeListeners(GoConfigHolder configHolder) {
//        saveValidConfigToCache(configHolder);
//        if(configHolder!=null) {
//            notifyListeners(currentConfig);
//        }
//    }

//    private synchronized void saveValidConfigToCache(GoConfigHolder configHolder) {
//        if (configHolder != null) {
//            LOGGER.debug("[Config Save] Saving config to the cache");
//            this.lastException = null;
//            this.configHolder = configHolder;
//            this.currentConfig = this.configHolder.config;
//            this.currentConfigForEdit = this.configHolder.configForEdit;
//            serverHealthService.update(ServerHealthState.success(invalidConfigType()));
//        }
//    }

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
        throw new RuntimeException("shouldn't get called");
//        GoConfigHolder newConfigHolder = dataSource.write(configFileContent, shouldMigrate);
//        saveValidConfigToCacheAndNotifyConfigChangeListeners(newConfigHolder);
    }
    public synchronized GoConfigHolder saveNew(String configFileContent, boolean shouldMigrate) throws Exception {
        return dataSource.write(configFileContent, shouldMigrate);
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
        throw new RuntimeException("shouldn't get called");
//        this.listeners.add(listener);
//        if (currentConfig != null) {
//            listener.onConfigChange(currentConfig);
//        }
    }

//    private synchronized void notifyListeners(CruiseConfig newCruiseConfig) {
//        LOGGER.info("About to notify config listeners");
//        for (ConfigChangedListener listener : listeners) {
//            try {
//                long startTime = System.currentTimeMillis();
//                listener.onConfigChange(newCruiseConfig);
//                LOGGER.debug("Notifying {} took (in ms): {}", listener.getClass(), (System.currentTimeMillis() - startTime));
//            } catch (Exception e) {
//                LOGGER.error("Failed to fire config changed event for listener: " + listener, e);
//            }
//        }
//        LOGGER.info("Finished notifying all listeners");
//    }

    /**
     * @deprecated Used only in tests
     */
    @Override
    public synchronized void clearListeners() {
        throw new RuntimeException("shouldn't get called");
        //TODO: jyoti, not required I think
//        listeners.clear();
    }

    /**
     * @deprecated Used only in tests
     */
    @Override
    public void reloadListeners() {
        throw new RuntimeException("shouldn't get called");
//TODO: jyoti, not required I think
//        notifyListeners(currentConfig());
    }

    @Override
    public GoConfigHolder loadConfigHolder() {

        throw new RuntimeException("shouldn't get called");
//        return configHolder;
    }

    @Override
    public boolean hasListener(ConfigChangedListener listener) {
        throw new RuntimeException("shouldn't get called");
        //TODO: jyoti, not required I think
//        return this.listeners.contains(listener);
    }
}
