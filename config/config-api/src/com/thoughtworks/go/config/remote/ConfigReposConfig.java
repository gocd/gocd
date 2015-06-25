package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * List of remote configuration sources and how to interpret them
 */
@ConfigTag("config-repos")
@ConfigCollection(value = ConfigRepoConfig.class)
public class ConfigReposConfig extends BaseCollection<ConfigRepoConfig> implements Validatable {

    private  ConfigErrors errors = new ConfigErrors();

    public ConfigReposConfig(){
    }

    public ConfigReposConfig(ConfigRepoConfig... configRepos)
    {
        for(ConfigRepoConfig repo : configRepos)
        {
            this.add(repo);
        }
    }

    public boolean hasMaterial(MaterialConfig materialConfig) {
        for(ConfigRepoConfig c : this)
        {
            if(c.getMaterialConfig().equals(materialConfig))
                return  true;
        }
        return  false;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validateMaterialUniqueness();
    }
    private void validateMaterialUniqueness() {
        Map<String, ConfigRepoConfig> materialHashMap = new HashMap<String, ConfigRepoConfig>();
        for (ConfigRepoConfig material : this) {
            material.validateMaterialUniqueness(materialHashMap);
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors().add(fieldName,message);
    }

    public ConfigRepoConfig getConfigRepo(MaterialConfig config) {
        for(ConfigRepoConfig repoConfig : this)
        {
            if(repoConfig.hasSameMaterial(config))
                return repoConfig;
        }
        return null;
    }

    public boolean hasMaterialWithFingerprint(String fingerprint) {
        for (ConfigRepoConfig repoConfig : this) {
            if (repoConfig.hasMaterialWithFingerprint(fingerprint))
                return true;
        }
        return false;
    }

    public boolean isReferenceAllowed(ConfigOrigin from, ConfigOrigin to) {

        if(isLocal(from) && !isLocal(to))
            return false;
        return true;
    }

    private boolean isLocal(ConfigOrigin from) {
        // we assume that configuration is local (from file or from UI) when origin is not specified
        if(from == null)
            return true;
        return from.isLocal();
    }
}