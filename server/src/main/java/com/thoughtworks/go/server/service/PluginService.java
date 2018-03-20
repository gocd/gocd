/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.server.dao.PluginDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PluginService {
    private final List<GoPluginExtension> extensions;
    private final PluginDao pluginDao;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private DefaultPluginInfoFinder defaultPluginInfoFinder;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);

    @Autowired
    public PluginService(List<GoPluginExtension> extensions, PluginDao pluginDao, SecurityService securityService, EntityHashingService entityHashingService, DefaultPluginInfoFinder defaultPluginInfoFinder) {
        this.extensions = extensions;
        this.pluginDao = pluginDao;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.defaultPluginInfoFinder = defaultPluginInfoFinder;
    }

    public PluginInfo pluginInfoForExtensionThatHandlesPluginSettings(String pluginId) {
        GoPluginExtension extension = findExtensionWhichCanHandleSettingsFor(pluginId);
        return extension == null ? null : defaultPluginInfoFinder.pluginInfoFor(pluginId).extensionFor(extension.extensionName());
    }

    public boolean isPluginLoaded(String pluginId) {
        for (GoPluginExtension extension : extensions) {
            if (extension.canHandlePlugin(pluginId)) {
                return true;
            }
        }
        return false;
    }

    public PluginSettings getPluginSettings(String pluginId) {
        Plugin plugin = pluginDao.findPlugin(pluginId);
        if (plugin instanceof NullPlugin) {
            return null;
        } else {
            return PluginSettings.from(plugin, pluginInfoForExtensionThatHandlesPluginSettings(pluginId));
        }
    }

    public void createPluginSettings(PluginSettings newPluginSettings, Username currentUser, LocalizedOperationResult result) {
        final String pluginId = newPluginSettings.getPluginId();
        if (hasPermission(currentUser, result) && checkPluginLoaded(pluginId, result) && checkPluginSupportsPluginSettings(pluginId, result)) {
            final String keyToLockOn = keyToLockOn(newPluginSettings.getPluginId());
            synchronized (keyToLockOn) {
                final Plugin plugin = pluginDao.findPlugin(newPluginSettings.getPluginId());
                if (plugin instanceof NullPlugin) {
                    updatePluginSettingsAndNotifyPluginSettingsChangeListeners(result, newPluginSettings);
                } else {
                    result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", String.format("The plugin settings for plugin[%s] is already exist. In order to update the plugin settings refer the https://api.gocd.org/%s/#update-plugin-settings.", newPluginSettings.getPluginId(), CurrentGoCDVersion.getInstance().goVersion())));
                }
            }
        }
    }

    public void updatePluginSettings(PluginSettings newPluginSettings, Username currentUser, LocalizedOperationResult result, String md5) {
        final String pluginId = newPluginSettings.getPluginId();

        if (hasPermission(currentUser, result) && checkPluginLoaded(pluginId, result) && checkPluginSupportsPluginSettings(pluginId, result)) {
            final String keyToLockOn = keyToLockOn(pluginId);
            synchronized (keyToLockOn) {
                final Plugin plugin = pluginDao.findPlugin(pluginId);
                if (plugin instanceof NullPlugin) {
                    result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Plugin Settings", pluginId), HealthStateType.notFound());
                    return;
                }

                final PluginSettings pluginSettingsFromDB = getPluginSettings(pluginId);
                if (!entityHashingService.md5ForEntity(pluginSettingsFromDB).equals(md5)) {
                    result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Plugin Settings", pluginId));
                    return;
                }

                updatePluginSettingsAndNotifyPluginSettingsChangeListeners(result, newPluginSettings);
                if (result.isSuccessful()) {
                    entityHashingService.removeFromCache(newPluginSettings, pluginId);
                }
            }
        }
    }

    void validatePluginSettingsFor(PluginSettings pluginSettings) {
        final String pluginId = pluginSettings.getPluginId();
        final PluginSettingsConfiguration configuration = pluginSettings.toPluginSettingsConfiguration();

        final GoPluginExtension extension = findExtensionWhichCanHandleSettingsFor(pluginId);
        if (extension == null) {
            throw new IllegalArgumentException(String.format("Plugin '%s' does not exist or does not implement settings validation.", pluginId));
        }

        final ValidationResult result = extension.validatePluginSettings(pluginId, configuration);
        if (!result.isSuccessful()) {
            for (ValidationError error : result.getErrors()) {
                pluginSettings.populateErrorMessageFor(error.getKey(), error.getMessage());
            }
        }
    }

    void saveOrUpdatePluginSettingsInDB(PluginSettings pluginSettings) {
        Plugin plugin = pluginDao.findPlugin(pluginSettings.getPluginId());
        if (plugin instanceof NullPlugin) {
            plugin = new Plugin(pluginSettings.getPluginId(), null);
        }
        Map<String, String> settingsMap = pluginSettings.getSettingsAsKeyValuePair();
        plugin.setConfiguration(toJSON(settingsMap));
        pluginDao.saveOrUpdate(plugin);
    }

    private void updatePluginSettingsAndNotifyPluginSettingsChangeListeners(LocalizedOperationResult result, PluginSettings pluginSettings) {
        synchronized (keyToLockOn(pluginSettings.getPluginId())) {
            try {
                validatePluginSettingsFor(pluginSettings);
                if (pluginSettings.hasErrors()) {
                    result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "There are errors in the plugin settings. Please fix them and resubmit."));
                    return;
                }
                saveOrUpdatePluginSettingsInDB(pluginSettings);
                notifyPluginSettingsChange(pluginSettings);
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getLocalizedMessage()));
                } else {
                    if (!result.hasMessage()) {
                        LOGGER.error(e.getMessage(), e);
                        result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the plugin settings. Please check the logs for more information."));
                    }
                }
            }
        }
    }

    private void notifyPluginSettingsChange(PluginSettings pluginSettings) {
        String pluginId = pluginSettings.getPluginId();

        GoPluginExtension extension = findExtensionWhichCanHandleSettingsFor(pluginId);
        if (extension == null) {
            LOGGER.trace("No extension handles plugin settings for plugin: {}", pluginId);
            return;
        }

        try {
            extension.notifyPluginSettingsChange(pluginId, pluginSettings.getSettingsAsKeyValuePair());
        } catch (Exception e) {
            LOGGER.warn("Error notifying plugin - {} with settings change", pluginId, e);
        }
    }

    private String toJSON(Map<String, String> settingsMap) {
        return new GsonBuilder().serializeNulls().create().toJson(settingsMap);
    }

    private String keyToLockOn(String pluginId) {
        return (getClass().getName() + "_plugin_settings_" + pluginId).intern();
    }

    private GoPluginExtension findExtensionWhichCanHandleSettingsFor(String pluginId) {
        String extensionWhichCanHandleSettings = PluginSettingsMetadataStore.getInstance().extensionWhichCanHandleSettings(pluginId);
        for (GoPluginExtension extension : extensions) {
            if (extension.extensionName().equals(extensionWhichCanHandleSettings) && extension.canHandlePlugin(pluginId)) {
                return extension;
            }
        }

        return null;
    }

    private boolean hasPermission(Username currentUser, LocalizedOperationResult result) {
        if (securityService.isUserAdmin(currentUser)) {
            return true;
        }

        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
        return false;
    }

    private boolean checkPluginLoaded(String pluginId, LocalizedOperationResult result) {
        if (!isPluginLoaded(pluginId)) {
            result.failedDependency(LocalizedMessage.string("FAILED_DEPENDENCY", String.format("The plugin with id %s is not loaded.", pluginId)));
            return false;
        }
        return true;
    }

    private boolean checkPluginSupportsPluginSettings(String pluginId, LocalizedOperationResult result) {
        final PluginInfo pluginInfo = pluginInfoForExtensionThatHandlesPluginSettings(pluginId);
        if (pluginInfo == null) {
            result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", String.format("The plugin with id %s does not support plugin settings.", pluginId)));
            return false;
        }
        return true;
    }
}
