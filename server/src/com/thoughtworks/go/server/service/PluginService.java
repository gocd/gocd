/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.dao.PluginDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.builder.PluginInfoBuilder;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
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
    private PluginInfoBuilder pluginInfoBuilder;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);

    @Autowired
    public PluginService(List<GoPluginExtension> extensions, PluginDao pluginDao, PluginInfoBuilder pluginInfoBuilder, SecurityService securityService, EntityHashingService entityHashingService) {
        this.extensions = extensions;
        this.pluginDao = pluginDao;
        this.pluginInfoBuilder = pluginInfoBuilder;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
    }

    public PluginSettings getPluginSettingsFor(String pluginId) {
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        Plugin plugin = pluginDao.findPlugin(pluginId);
        if (plugin instanceof NullPlugin) {
            pluginSettings.populateSettingsMap(PluginSettingsMetadataStore.getInstance().configuration(pluginId));
        } else {
            pluginSettings.populateSettingsMap(plugin);
        }
        return pluginSettings;
    }

    public PluginSettings getPluginSettings(String pluginId) {
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        Plugin plugin = pluginDao.findPlugin(pluginId);
        if (plugin instanceof NullPlugin) {
            return null;
        } else {
            pluginSettings.populateSettingsMap(plugin);
        }
        return pluginSettings;
    }

    public void createPluginSettings(Username currentUser, LocalizedOperationResult result, PluginSettings pluginSettings) {
        update(currentUser, result, pluginSettings);
    }

    public void updatePluginSettings(Username currentUser, LocalizedOperationResult result, PluginSettings pluginSettings, String md5) {
        String keyToLockOn = keyToLockOn(pluginSettings.getPluginId());
        synchronized (keyToLockOn) {
            if (isRequestFresh(md5, pluginSettings, result)) {
                update(currentUser, result, pluginSettings);
                if (result.isSuccessful()) {
                    entityHashingService.removeFromCache(pluginSettings, pluginSettings.getPluginId());
                }
            }
        }
    }

    public PluginSettings getPluginSettingsFor(String pluginId, Map<String, String> parameterMap) {
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        pluginSettings.populateSettingsMap(parameterMap);
        return pluginSettings;
    }

    public void validatePluginSettingsFor(PluginSettings pluginSettings) {
        String pluginId = pluginSettings.getPluginId();
        PluginSettingsConfiguration configuration = pluginSettings.toPluginSettingsConfiguration();
        ValidationResult result = null;

        boolean anyExtensionSupportsPluginId = false;
        for (GoPluginExtension extension : extensions) {
            if (extension.canHandlePlugin(pluginId)) {
                result = extension.validatePluginSettings(pluginId, configuration);
                anyExtensionSupportsPluginId = true;
            }
        }
        if (!anyExtensionSupportsPluginId)
            throw new IllegalArgumentException(String.format(
                    "Plugin '%s' is not supported by any extension point", pluginId));

        if (!result.isSuccessful()) {
            for (ValidationError error : result.getErrors()) {
                pluginSettings.populateErrorMessageFor(error.getKey(), error.getMessage());
            }
        }
    }

    public void savePluginSettingsFor(PluginSettings pluginSettings) {
        Plugin plugin = pluginDao.findPlugin(pluginSettings.getPluginId());
        if (plugin instanceof NullPlugin) {
            plugin = new Plugin(pluginSettings.getPluginId(), null);
        }
        Map<String, String> settingsMap = pluginSettings.getSettingsAsKeyValuePair();
        plugin.setConfiguration(toJSON(settingsMap));
        pluginDao.saveOrUpdate(plugin);
    }

    private void update(Username currentUser, LocalizedOperationResult result, PluginSettings pluginSettings) {
        synchronized (keyToLockOn(pluginSettings.getPluginId())) {
            if (!securityService.isUserAdmin(currentUser)) {
                result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            } else {
                try {
                    validatePluginSettingsFor(pluginSettings);
                    if (pluginSettings.hasErrors()) {
                        result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "There are errors in the plugin settings. Please fix them and resubmit."));
                        return;
                    }
                    savePluginSettingsFor(pluginSettings);
                } catch (Exception e) {
                    if (e instanceof IllegalArgumentException) {
                        result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getLocalizedMessage()));
                    } else {
                        if (!result.hasMessage()) {
                            LOGGER.error(e.getMessage(), e);
                            result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the template config. Please check the logs for more information."));
                        }
                    }
                }
            }
        }
    }

    @Deprecated // used by v1 and v2
    public List<PluginInfo> pluginInfos(String type) {
        return pluginInfoBuilder.allPluginInfos(type);
    }

    @Deprecated
    public PluginInfo pluginInfo(String pluginId) {
        return pluginInfoBuilder.pluginInfoFor(pluginId);
    }

    private String toJSON(Map<String, String> settingsMap) {
        return new GsonBuilder().serializeNulls().create().toJson(settingsMap);
    }

    private String keyToLockOn(String pluginId) {
        return (getClass().getName() + "_plugin_settings_" + pluginId).intern();
    }

    private boolean isRequestFresh(String md5, PluginSettings pluginSettings, LocalizedOperationResult result) {
        PluginSettings storedPluginSettings = getPluginSettings(pluginSettings.getPluginId());
        if (storedPluginSettings == null) {
            result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Plugin Settings", pluginSettings.getPluginId()), HealthStateType.notFound());
            return false;
        }
        boolean freshRequest = entityHashingService.md5ForEntity(pluginSettings).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Plugin Settings", pluginSettings.getPluginId()));
        }
        return freshRequest;

    }

}
