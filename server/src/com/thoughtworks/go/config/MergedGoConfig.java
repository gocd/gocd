/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.remote.PartialConfig;
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

/**
 * @understands when to reload the config file or other config source
 */
@Component
public class MergedGoConfig implements CachedGoConfig, ConfigChangedListener, PartialConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileGoConfig.class);

    public static final String INVALID_CRUISE_CONFIG_MERGE = "Invalid Merged Configuration";

    private CachedFileGoConfig fileService;
    private GoPartialConfig partialConfig;

    private final ServerHealthService serverHealthService;
    private List<ConfigChangedListener> listeners = new ArrayList<ConfigChangedListener>();

    // this is merged config when possible
    private volatile CruiseConfig currentConfig;
    private volatile CruiseConfig currentConfigForEdit;
    private volatile GoConfigHolder configHolder;
    private volatile Exception lastException;

    @Autowired public MergedGoConfig(ServerHealthService serverHealthService,
                                     CachedFileGoConfig fileService, GoPartialConfig partialConfig) {
        this.serverHealthService = serverHealthService;
        this.fileService = fileService;
        this.partialConfig = partialConfig;

        this.fileService.registerListener(this);
        this.partialConfig.registerListener(this);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        this.tryAssembleMergedConfig(this.fileService.loadConfigHolder(), this.partialConfig.lastPartials());
    }
    @Override
    public void onPartialConfigChanged(List<PartialConfig> partials) {
        this.tryAssembleMergedConfig(this.fileService.loadConfigHolder(), partials);
    }

    /**
     * attempts to create a new merged cruise config
    */
    public void tryAssembleMergedConfig(GoConfigHolder cruiseConfigHolder,List<PartialConfig> partials) {
        try {
            GoConfigHolder newConfigHolder;

            if (partials.size() == 0) {
                // no partial configurations
                // then just use basic configuration from xml
                newConfigHolder = cruiseConfigHolder;
            } else {
                // create merge (uses merge strategy internally)
                BasicCruiseConfig merge = new BasicCruiseConfig((BasicCruiseConfig) cruiseConfigHolder.config, partials);
                // validate
                List<ConfigErrors> allErrors = validate(merge);
                if (!allErrors.isEmpty()) {
                    throw new GoConfigInvalidException(merge, allErrors);
                }
                CruiseConfig basicForEdit = this.fileService.loadForEditing();
                CruiseConfig forEdit = new BasicCruiseConfig((BasicCruiseConfig) basicForEdit, partials);
                //TODO change strategy into merge-edit? - done in UI branch
                newConfigHolder = new GoConfigHolder(merge, forEdit);
            }
            // save to cache and fire event
            this.saveValidConfigToCacheAndNotifyConfigChangeListeners(newConfigHolder);
        } catch (Exception e) {
            LOGGER.error("Failed validation of merged configuration: {}", e.toString());
            saveConfigError(e);
        }
    }
    private synchronized void saveConfigError(Exception e) {
        this.lastException = e;
        ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_MERGE, GoConfigValidity.invalid(e).errorMessage(), invalidConfigType());
        serverHealthService.update(state);
    }

    public static List<ConfigErrors> validate(CruiseConfig config) {
        List<ConfigErrors> validationErrors = new ArrayList<ConfigErrors>();
        validationErrors.addAll(config.validateAfterPreprocess());
        return validationErrors;
    }

    // used in tests
    public void forceReload() {
        this.fileService.onTimer();
    }

    public CruiseConfig loadForEditing() {
        // merged cannot be (entirely) edited but we return it so that all pipelines are rendered in admin->pipelines
        return currentConfigForEdit;
    }

    public CruiseConfig currentConfig() {
        //returns merged cruise config if appropriate
        if (currentConfig == null) {
            currentConfig = new BasicCruiseConfig();
        }
        return currentConfig;
    }

    public void loadConfigIfNull() {
        this.fileService.loadConfigIfNull();
    }

    // no actions on timer now. We only react to events in CachedFileGoConfig and in GoPartialConfig

    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand) {
        return this.fileService.writeWithLock(updateConfigCommand,new GoConfigHolder(this.currentConfig,this.currentConfigForEdit));
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
            if(listener instanceof PipelineConfigChangedListener){
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
            serverHealthService.update(ServerHealthState.success(invalidConfigType()));
        }
    }
    private synchronized void saveValidConfigToCacheAndNotifyConfigChangeListeners(GoConfigHolder configHolder) {
        saveValidConfigToCache(configHolder);
        if(configHolder!=null) {
            notifyListeners(currentConfig);
        }
    }


    private synchronized void saveValidConfigToCacheTomas(GoConfigHolder configHolder) {
        //this operation still exists, it only works differently
        // we validate entire merged cruise config
        // then we keep new merged cruise config in memory
        // then we take out main part of it and keep it for edits
        LOGGER.debug("[Config Save] Saving (merged) config to the cache");
        this.configHolder = configHolder;
        // this is merged or basic config.
        this.currentConfig = this.configHolder.config;
        this.currentConfigForEdit = this.configHolder.configForEdit;
        serverHealthService.update(ServerHealthState.success(invalidConfigType()));
        LOGGER.info("About to notify (merged) config listeners");
        // but we do notify with merged config
        notifyListeners(currentConfig);
        LOGGER.info("Finished notifying all listeners");
    }

    private static HealthStateType invalidConfigType() {
        return HealthStateType.invalidConfigMerge();
    }

    public String getFileLocation() {
        return this.fileService.getFileLocation();
    }

    public void save(String configFileContent, boolean shouldMigrate) throws Exception {
        // this will save new xml, and notify me (as listener) so that I will attempt to update my merged config as well.
        this.fileService.save(configFileContent,shouldMigrate);
    }

    public GoConfigValidity checkConfigFileValid() {
        return  this.fileService.checkConfigFileValid();
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
