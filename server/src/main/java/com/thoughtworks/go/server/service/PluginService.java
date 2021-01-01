/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
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

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

@Service
public class PluginService {
    private final List<GoPluginExtension> extensions;
    private final PluginDao pluginDao;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private DefaultPluginInfoFinder defaultPluginInfoFinder;
    private final PluginManager pluginManager;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);

    @Autowired
    public PluginService(List<GoPluginExtension> extensions, PluginDao pluginDao, SecurityService securityService,
                         EntityHashingService entityHashingService, DefaultPluginInfoFinder defaultPluginInfoFinder,
                         PluginManager pluginManager) {

        this.extensions = extensions;
        this.pluginDao = pluginDao;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.defaultPluginInfoFinder = defaultPluginInfoFinder;
        this.pluginManager = pluginManager;
    }

    public PluginInfo pluginInfoForExtensionThatHandlesPluginSettings(String pluginId) {
        GoPluginExtension extension = findExtensionWhichCanHandleSettingsFor(pluginId);
        return extension == null ? null : defaultPluginInfoFinder.pluginInfoFor(pluginId).extensionFor(extension.extensionName());
    }

    public boolean isPluginLoaded(String pluginId) {
        return pluginManager.isPluginLoaded(pluginId);
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
        final String keyToLockOn = keyToLockOn(newPluginSettings.getPluginId());
        synchronized (keyToLockOn) {
            if (hasPermission(currentUser, newPluginSettings.getPluginId(), result)) {
                final Plugin plugin = pluginDao.findPlugin(newPluginSettings.getPluginId());
                if (plugin instanceof NullPlugin) {
                    updatePluginSettingsAndNotifyPluginSettingsChangeListeners(result, newPluginSettings);
                } else {
                    result.unprocessableEntity(LocalizedMessage.saveFailedWithReason(String.format("Plugin settings for the plugin `%s` already exist. In order to update the plugin settings refer the %s.", newPluginSettings.getPluginId(), apiDocsUrl("#update-plugin-settings"))));
                }
            }
        }

    }

    public void updatePluginSettings(PluginSettings newPluginSettings, Username currentUser, LocalizedOperationResult result, String digest) {
        final String pluginId = newPluginSettings.getPluginId();

        final String keyToLockOn = keyToLockOn(pluginId);
        synchronized (keyToLockOn) {
            if (hasPermission(currentUser, newPluginSettings.getPluginId(), result)) {
                final PluginSettings pluginSettingsFromDB = getPluginSettings(pluginId);
                if (pluginSettingsFromDB == null) {
                    result.notFound(EntityType.PluginSettings.notFoundMessage(pluginId), HealthStateType.notFound());
                    return;
                }
                if (!entityHashingService.hashForEntity(pluginSettingsFromDB).equals(digest)) {
                    result.stale(EntityType.PluginSettings.staleConfig(pluginId));
                    return;
                }

                updatePluginSettingsAndNotifyPluginSettingsChangeListeners(result, newPluginSettings);
                if (result.isSuccessful()) {
                    entityHashingService.removeFromCache(newPluginSettings, pluginId);
                }
            }
        }
    }

    void validatePluginSettings(PluginSettings pluginSettings) {
        pluginSettings.validateTree();
        if (pluginSettings.hasErrors()) {
            return;
        }

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
                validatePluginSettings(pluginSettings);
                if (pluginSettings.hasErrors()) {
                    result.unprocessableEntity(LocalizedMessage.saveFailedWithReason("There are errors in the plugin settings. Please fix them and resubmit."));
                    return;
                }
                saveOrUpdatePluginSettingsInDB(pluginSettings);
                notifyPluginSettingsChange(pluginSettings);
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    result.unprocessableEntity(LocalizedMessage.saveFailedWithReason(e.getLocalizedMessage()));
                } else {
                    if (!result.hasMessage()) {
                        LOGGER.error(e.getMessage(), e);
                        result.internalServerError(LocalizedMessage.saveFailedWithReason("An error occurred while saving the plugin settings. Please check the logs for more information."));
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

    private boolean hasPermission(Username currentUser, String pluginId, LocalizedOperationResult result) {
        if (securityService.isUserAdmin(currentUser)) {
            return true;
        }

        result.forbidden(EntityType.PluginSettings.forbiddenToEdit(pluginId, currentUser.getUsername()), HealthStateType.forbidden());
        return false;
    }
}
