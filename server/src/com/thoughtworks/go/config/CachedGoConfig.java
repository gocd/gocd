/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.merge.MergeCruiseConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.server.service.GoConfigService.INVALID_CRUISE_CONFIG_XML;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands when to reload the config file or other config source
 */
@Component
public class CachedGoConfig implements ConfigChangedListener, PartialConfigChangedListener {
    private static final Logger LOGGER = Logger.getLogger(CachedGoConfig.class);

    //TODO #1133 use GoPartialConfig and CachedFileGoConfig to provide all services that old CachedGoConfig
    private CachedFileGoConfig fileService;
    private GoPartialConfig partialConfig;

    private final GoFileConfigDataSource dataSource;
    private final ServerHealthService serverHealthService;
    private List<ConfigChangedListener> listeners = new ArrayList<ConfigChangedListener>();

    // this is merged config when possible
    private volatile CruiseConfig currentConfig;
    private volatile CruiseConfig currentConfigForEdit;
    private volatile Exception lastException;
    private volatile GoConfigHolder configHolder;

    @Autowired public CachedGoConfig(GoFileConfigDataSource dataSource, ServerHealthService serverHealthService,
                                     CachedFileGoConfig fileService,GoPartialConfig partialConfig) {
        this.dataSource = dataSource;
        this.serverHealthService = serverHealthService;
        this.fileService = fileService;
        this.partialConfig = partialConfig;

        this.fileService.registerListener(this);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        this.tryAssembleMergedConfig(newCruiseConfig,this.partialConfig.lastPartials());
    }
    @Override
    public void onPartialConfigChanged(PartialConfig[] partials) {
        this.tryAssembleMergedConfig(this.fileService.currentConfig(),partials);
    }

    /**
     * attempts to create a new merged cruise config
    */
    private void tryAssembleMergedConfig(CruiseConfig cruiseConfig,PartialConfig[] partials)
    {
        // create merge (by new MergeCruiseConfig or by injecting strategy)
        // validate
        // save to cache and fire event
    }


    public void forceReload() {

    }

    public CruiseConfig loadForEditing() {
        //here we will return main CruiseConfig because merged cannot be (entirely) edited
        return fileService.loadForEditing();
    }

    public CruiseConfig currentConfig() {
        //returns merged cruise config if appropriate
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

    public void loadConfigIfNull() {
        this.fileService.loadConfigIfNull();
    }

    // no actions on timer now. We only react to events in CachedFileGoConfig and in GoPartialConfig

    public synchronized ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand) {
        GoFileConfigDataSource.GoConfigSaveResult saveResult = dataSource.writeWithLock(updateConfigCommand, new GoConfigHolder(currentConfig, currentConfigForEdit));
        saveValidConfigToCache(saveResult.getConfigHolder());
        return saveResult.getConfigSaveState();
    }

    private synchronized void saveValidConfigToCache(GoConfigHolder configHolder) {
        //this operation still exists, it only works differently
        // we validate entire merged cruise config
        // then we keep new merged cruise config in memory
        // then we take out main part of it and keep it for edits
        if (configHolder != null) {
            LOGGER.debug("[Config Save] Saving config to the cache");
            this.lastException = null;
            this.configHolder = configHolder;
            this.currentConfig = this.configHolder.config;
            this.currentConfigForEdit = this.configHolder.configForEdit;
            serverHealthService.update(ServerHealthState.success(invalidConfigType()));
            LOGGER.info("About to notify config listeners");
            // but we do notify with merged config
            notifyListeners(currentConfig);
            LOGGER.info("Finished notifying all listeners");
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

    public String getFileLocation() {
        return dataSource.fileLocation().getAbsolutePath();
    }

    public synchronized void save(String configFileContent, boolean shouldMigrate) throws Exception {
        GoConfigHolder newConfigHolder = dataSource.write(configFileContent, shouldMigrate);
        //TODO now attempt merge again so that holder has merge cruise config with new main
        saveValidConfigToCache(newConfigHolder);
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
        for (ConfigChangedListener listener : listeners) {
            try {
                listener.onConfigChange(newCruiseConfig);
            } catch (Exception e) {
                LOGGER.error("failed to fire config changed event for listener: " + listener, e);
            }
        }
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


}
