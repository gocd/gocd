package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutListener;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses partial configurations and exposes latest configurations as soon as possible.
 */
@Component
public class GoRepoConfigDataSource implements ChangedRepoConfigWatchListListener, ScmMaterialCheckoutListener {
    private static final Logger LOGGER = Logger.getLogger(GoRepoConfigDataSource.class);

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;

    private Map<String,PartialConfig> fingerprintLatestConfigMap = new ConcurrentHashMap<String,PartialConfig>();

    @Autowired public GoRepoConfigDataSource(GoConfigWatchList configWatchList,GoConfigPluginService configPluginService,
                                             ScmMaterialCheckoutService checkoutService)
    {
        this.configPluginService = configPluginService;
        this.configWatchList = configWatchList;
        this.checkoutService = checkoutService;
        this.checkoutService.registerListener(this);
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos)
    {
        // TODO remove partial configs from map which are no longer on the list

    }

    @Override
    public void onCheckoutComplete(Material material, List<Modification> newChanges, File folder, String revision) {
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
            ConfigRepoConfig repoConfig = configWatchList.getConfigRepoForMaterial(material.config());
            PartialConfigProvider plugin = this.configPluginService.partialConfigProviderFor(repoConfig);

            //TODO put modifications and previous partial config in context
            // the context is just a helper for plugin.
            PartialConfigLoadContext context = null;
            try {
                PartialConfig newPart = plugin.Load(folder, context);
                if(newPart == null)
                {
                    LOGGER.warn(String.format("Parsed configuration material %s by %s is null",
                            material.getDisplayName(), plugin));
                    newPart = new PartialConfig();
                }

                newPart.setOrigin(new RepoConfigOrigin(repoConfig,revision));
                fingerprintLatestConfigMap.put(fingerprint, newPart);
                //TODO post message about finished parsed part
            }
            catch (Exception ex)
            {
                // TODO make sure this is clearly shown to user
                LOGGER.error(String.format("Failed to parse configuration material %s by %s",
                        material.getDisplayName(),plugin));
                //TODO post message about failed parsed part
            }
        }
    }
}
