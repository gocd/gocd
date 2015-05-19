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

package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;

@Component
public class PluginSettingsMetadataLoader implements PluginChangeListener {
    private static final Logger LOGGER = getLogger(PluginSettingsMetadataLoader.class);

    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private SCMExtension scmExtension;
    private TaskExtension taskExtension;
    private NotificationExtension notificationExtension;
    private AuthenticationExtension authenticationExtension;
    private PluginSettingsMetadataStore metadataStore = PluginSettingsMetadataStore.getInstance();

    @Autowired
    public PluginSettingsMetadataLoader(PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension,
                                        TaskExtension taskExtension, NotificationExtension notificationExtension,
                                        AuthenticationExtension authenticationExtension, PluginManager pluginManager) {
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.taskExtension = taskExtension;
        this.notificationExtension = notificationExtension;
        this.authenticationExtension = authenticationExtension;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        fetchPluginSettingsMetaData(pluginDescriptor);
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        metadataStore.removeMetadataFor(pluginDescriptor.id());
    }

    void fetchPluginSettingsMetaData(GoPluginDescriptor pluginDescriptor) {
        String pluginId = pluginDescriptor.id();
        try {
            PluginSettingsConfiguration configuration = null;
            String view = null;
            if (packageAsRepositoryExtension.isPackageRepositoryPlugin(pluginId)) {
                configuration = packageAsRepositoryExtension.getPluginSettingsConfiguration(pluginId);
                view = packageAsRepositoryExtension.getPluginSettingsView(pluginId);
            } else if (scmExtension.isSCMPlugin(pluginId)) {
                configuration = scmExtension.getPluginSettingsConfiguration(pluginId);
                view = scmExtension.getPluginSettingsView(pluginId);
            } else if (taskExtension.isTaskPlugin(pluginId)) {
                configuration = taskExtension.getPluginSettingsConfiguration(pluginId);
                view = taskExtension.getPluginSettingsView(pluginId);
            } else if (notificationExtension.isNotificationPlugin(pluginId)) {
                configuration = notificationExtension.getPluginSettingsConfiguration(pluginId);
                view = notificationExtension.getPluginSettingsView(pluginId);
            } else if (authenticationExtension.isAuthenticationPlugin(pluginId)) {
                configuration = authenticationExtension.getPluginSettingsConfiguration(pluginId);
                view = authenticationExtension.getPluginSettingsView(pluginId);
            }
            if (configuration == null || view == null) {
                throw new RuntimeException("Plugin Settings - Configuration or View cannot be null");
            }
            metadataStore.addMetadataFor(pluginId, configuration, view);
        } catch (Exception e) {
            LOGGER.error(format("Failed to fetch Plugin Settings metadata for plugin : %s", pluginId), e);
        }
    }
}
