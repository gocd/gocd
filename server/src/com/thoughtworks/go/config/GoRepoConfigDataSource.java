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

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutListener;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * Parses partial configurations and exposes latest configurations as soon as possible.
 */
@Component
public class GoRepoConfigDataSource implements ChangedRepoConfigWatchListListener, ScmMaterialCheckoutListener {
    private static final Logger LOGGER = Logger.getLogger(GoRepoConfigDataSource.class);

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;

    // value is partial config instance or last exception
    private Map<String,PartialConfigParseResult> fingerprintOfPartialToLatestParseResultMap = new ConcurrentHashMap<String,PartialConfigParseResult>();

    private List<PartialConfigUpdateCompletedListener> listeners = new ArrayList<PartialConfigUpdateCompletedListener>();

    @Autowired public GoRepoConfigDataSource(GoConfigWatchList configWatchList,GoConfigPluginService configPluginService,
                                             ScmMaterialCheckoutService checkoutService)
    {
        this.configPluginService = configPluginService;
        this.configWatchList = configWatchList;
        this.checkoutService = checkoutService;
        this.checkoutService.registerListener(this);
        this.configWatchList.registerListener(this);
    }

    public boolean hasListener(PartialConfigUpdateCompletedListener listener) {
        return this.listeners.contains(listener);
    }
    public void registerListener(PartialConfigUpdateCompletedListener listener) {
        this.listeners.add(listener);
    }

    public boolean latestParseHasFailedForMaterial(MaterialConfig material) {
        String fingerprint = material.getFingerprint();
        PartialConfigParseResult result = fingerprintOfPartialToLatestParseResultMap.get(fingerprint);
        if (result == null)
            return false;
        return  result.getLastFailure() != null;
    }

    public PartialConfig latestPartialConfigForMaterial(MaterialConfig material) throws Exception
    {
        String fingerprint = material.getFingerprint();
        PartialConfigParseResult result = fingerprintOfPartialToLatestParseResultMap.get(fingerprint);
        if(result == null)
            return  null;
        if(result.getLastFailure() != null)
            throw result.getLastFailure();

        return result.getLastSuccess();
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos)
    {
        // remove partial configs from map which are no longer on the list
        for(String fingerprint : this.fingerprintOfPartialToLatestParseResultMap.keySet())
        {
            if(!newConfigRepos.hasMaterialWithFingerprint(fingerprint))
            {
                this.fingerprintOfPartialToLatestParseResultMap.remove(fingerprint);
            }
        }
    }

    @Override
    public void onCheckoutComplete(MaterialConfig material, File folder, String revision) {
        // called when pipelines/flyweight/[flyweight] has a clean checkout of latest material

        // Having modifications in signature might seem like an overkill
        // but on the other hand if plugin is smart enough it could
        // parse only files that have changed, which is a huge performance gain where there are many pipelines

        /* if this is material listed in config-repos
           Then ask for config plugin implementation
           Give it the directory and store partial config
           post event about completed (successful or not) parsing
         */

        String fingerprint = material.getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            PartialConfigProvider plugin = null;
            ConfigRepoConfig repoConfig = configWatchList.getConfigRepoForMaterial(material);
            try {
                plugin = this.configPluginService.partialConfigProviderFor(repoConfig);
            }
            catch (Exception ex)
            {
                // TODO make sure this is clearly shown to user
                fingerprintOfPartialToLatestParseResultMap.put(fingerprint, new PartialConfigParseResult(ex));
                LOGGER.error(String.format("Failed to get config plugin for %s",
                        material.getDisplayName()));
                notifyFailureListeners(repoConfig, ex);
                return;
            }
            try {
                //TODO put modifications and previous partial config in context
                // the context is just a helper for plugin.
                PartialConfigLoadContext context = new LoadContext();
                PartialConfig newPart = plugin.load(folder, context);
                if(newPart == null)
                {
                    LOGGER.warn(String.format("Parsed configuration material %s by %s is null",
                            material.getDisplayName(), plugin));
                    newPart = new PartialConfig();
                }

                newPart.setOrigins(new RepoConfigOrigin(repoConfig,revision));
                fingerprintOfPartialToLatestParseResultMap.put(fingerprint, new PartialConfigParseResult(newPart));
                notifySuccessListeners(repoConfig, newPart);
            }
            catch (Exception ex)
            {
                // TODO make sure this is clearly shown to user
                fingerprintOfPartialToLatestParseResultMap.put(fingerprint, new PartialConfigParseResult(ex));
                LOGGER.error(String.format("Failed to parse configuration material %s by %s",
                        material.getDisplayName(),plugin));
                notifyFailureListeners(repoConfig, ex);
            }
        }
    }

    private void notifyFailureListeners(ConfigRepoConfig repoConfig, Exception ex) {
        for(PartialConfigUpdateCompletedListener listener : this.listeners)
        {
            try
            {
                listener.onFailedPartialConfig(repoConfig,ex);
            }
            catch (Exception e)
            {
                LOGGER.error(String.format("Failed to fire event 'exception while parsing partial configuration' for listener %s",
                        listener));
            }
        }
    }

    private void notifySuccessListeners(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        for(PartialConfigUpdateCompletedListener listener : this.listeners)
        {
            try
            {
                listener.onSuccessPartialConfig(repoConfig,newPart);
            }
            catch (Exception e)
            {
                LOGGER.error(String.format("Failed to fire parsed partial configuration for listener %s",
                        listener));
            }
        }
    }



    private  class  PartialConfigParseResult{
        private PartialConfig lastSuccess;
        private Exception lastFailure;

        public PartialConfigParseResult(PartialConfig newPart) {
            this.lastSuccess = newPart;
        }

        public PartialConfigParseResult(Exception ex) {
            this.lastFailure = ex;
        }

        public PartialConfig getLastSuccess() {
            return lastSuccess;
        }

        public void setLastSuccess(PartialConfig lastSuccess) {
            this.lastSuccess = lastSuccess;
        }

        public Exception getLastFailure() {
            return lastFailure;
        }

        public void setLastFailure(Exception lastFailure) {
            this.lastFailure = lastFailure;
        }
    }

    private class LoadContext implements PartialConfigLoadContext
    {

    }
}
