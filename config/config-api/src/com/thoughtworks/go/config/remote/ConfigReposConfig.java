package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.materials.MaterialConfig;

/**
 * List of remote configuration sources and how to interpret them
 */
public class ConfigReposConfig extends BaseCollection<ConfigRepoConfig> {

    public boolean hasMaterial(MaterialConfig materialConfig) {
        for(ConfigRepoConfig c : this)
        {
            if(c.getMaterialConfig().equals(materialConfig))
                return  true;
        }
        return  false;
    }
}