package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.listener.ConfigChangedListener;

public interface CachedGoConfig {
    CruiseConfig loadForEditing();

    CruiseConfig currentConfig();

    void forceReload();

    void loadConfigIfNull();

    ConfigSaveState writeWithLock(UpdateConfigCommand updateConfigCommand);

    String getFileLocation();

    void save(String configFileContent, boolean shouldMigrate) throws Exception;

    GoConfigValidity checkConfigFileValid();

    void registerListener(ConfigChangedListener listener);

    void clearListeners();

    void reloadListeners();

    GoConfigHolder loadConfigHolder();

    boolean hasListener(ConfigChangedListener listener);
}
