package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.StringUtil;

import java.util.Map;

/**
 * Defines single source of remote configuration and name of plugin to interpet it.
 * This goes to standard static xml configuration.
 */
@ConfigTag("config-repo")
public class ConfigRepoConfig implements Validatable {
    // defines source of configuration. Any will fit
    @ConfigSubtag(optional = false)
    private MaterialConfig repo;

    @ConfigSubtag
    private Configuration configuration = new Configuration();

    // TODO something must instantiate this name into proper implementation of ConfigProvider
    // which can be a plugin or embedded class
    @ConfigAttribute(value = "plugin", allowNull = false)
    private String configProviderPluginName = "gocd-xml";
    // plugin-name which will process the repository tree to return configuration.
    // as in https://github.com/gocd/gocd/issues/1133#issuecomment-109014208
    // then pattern-based plugin is just one option

    public static final String UNIQUE_REPO = "unique_repo";
    public static final String REPO = "repo";

    private ConfigErrors errors = new ConfigErrors();

    public ConfigRepoConfig(){
    }
    public ConfigRepoConfig(MaterialConfig repo, String configProviderPluginName){
        this.repo = repo;
        this.configProviderPluginName = configProviderPluginName;
    }

    public MaterialConfig getMaterialConfig() {
        return repo;
    }

    public void setMaterialConfig(ScmMaterialConfig config) {
        this.repo = config;
    }

    public String getConfigProviderPluginName() {
        return configProviderPluginName;
    }

    public void setConfigProviderPluginName(String configProviderPluginName) {
        if(StringUtil.isBlank(configProviderPluginName))
            configProviderPluginName = null;
        this.configProviderPluginName = configProviderPluginName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigRepoConfig that = (ConfigRepoConfig) o;

        if (repo != null ? !repo.equals(that.repo) : that.repo != null) {
            return false;
        }
        if (configProviderPluginName != null ? !configProviderPluginName.equals(that.configProviderPluginName) : that.configProviderPluginName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = repo != null ? repo.hashCode() : 0;
        result = 31 * result + (configProviderPluginName != null ? configProviderPluginName.hashCode() : 0);
        return result;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validateRepoIsSet();
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors.add(fieldName,message);
    }

    public void validateMaterialUniqueness(Map<String, ConfigRepoConfig> map) {
        if (this.getMaterialConfig() == null) {
            return;
        }
        String materialFingerprint = this.getMaterialConfig().getFingerprint();
        ConfigRepoConfig repoWithSameFingerprint = map.get(materialFingerprint);
        if (repoWithSameFingerprint != null) {
            repoWithSameFingerprint.addMaterialConflictError();
            addMaterialConflictError();
            return;
        }
        map.put(materialFingerprint, this);
    }

    private void addMaterialConflictError() {
        this.errors.add(UNIQUE_REPO,String.format(
                "You have defined multiple configuration repositories with the same repository - %s",
                this.repo.getDisplayName()));
    }

    private void validateRepoIsSet() {
        if (this.getMaterialConfig() == null) {
            this.errors.add(REPO,"Configuration repository material not specified");
        }
    }

    public boolean hasSameMaterial(MaterialConfig config) {
        if(this.getMaterialConfig() == null)
            return  false;
        String materialFingerprint = this.getMaterialConfig().getFingerprint();
        return materialFingerprint.equals(config.getFingerprint());
    }

    public boolean hasMaterialWithFingerprint(String fingerprint) {
        if (this.getMaterialConfig() == null) {
            return false;
        }
        String materialFingerprint = this.getMaterialConfig().getFingerprint();
        return materialFingerprint.equals(fingerprint);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
