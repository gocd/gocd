/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.commons.collections4.IterableUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * This knows what needs to be done when a plugin is loaded (and unloaded).
 */

@Component
public class PluginLoader {
    private Collection<PluginChangeListener> pluginChangeListeners = new ConcurrentLinkedQueue<>();
    private List<PluginPostLoadHook> pluginPostLoadHooks = new ArrayList<>();
    private GoPluginOSGiFramework pluginOSGiFramework;
    private Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);

    @Autowired
    public PluginLoader(GoPluginOSGiFramework pluginOSGiFramework) {
        this.pluginOSGiFramework = pluginOSGiFramework;
    }

    public void addPluginChangeListener(PluginChangeListener pluginChangeListener) {
        pluginChangeListeners.add(pluginChangeListener);
    }

    public PluginPostLoadHook addPluginPostLoadHook(PluginPostLoadHook pluginPostLoadHook) {
        pluginPostLoadHooks.add(pluginPostLoadHook);
        return pluginPostLoadHook;
    }

    public void loadPlugin(GoPluginBundleDescriptor descriptor) {
        try {
            pluginOSGiFramework.loadPlugin(descriptor);

            if (descriptor.isInvalid()) {
                handlePluginInvalidation(descriptor, descriptor.bundleLocation());
                return;
            }

            doPostBundleInstallActivities(descriptor);
        } catch (Exception e) {
            File bundleLocation = descriptor.bundleLocation();
            descriptor.markAsInvalid(List.of(e.getMessage()), e);
            LOGGER.error("Failed to load plugin: {}", bundleLocation, e);
            handlePluginInvalidation(descriptor, bundleLocation);
            throw new RuntimeException("Failed to load plugin: " + bundleLocation, e);
        }
    }

    public void unloadPlugin(GoPluginBundleDescriptor descriptorOfRemovedPlugin) {
        Bundle bundle = descriptorOfRemovedPlugin.bundle();
        if (bundle == null) {
            return;
        }

        for (GoPluginDescriptor pluginDescriptor : descriptorOfRemovedPlugin.descriptors()) {
            for (PluginChangeListener listener : pluginChangeListeners) {
                try {
                    listener.pluginUnLoaded(pluginDescriptor);
                } catch (Exception e) {
                    LOGGER.warn("A plugin unload listener ({}) failed: {}", listener.toString(), pluginDescriptor, e);
                }
            }
        }

        pluginOSGiFramework.unloadPlugin(descriptorOfRemovedPlugin);
    }

    private void doPostBundleInstallActivities(GoPluginBundleDescriptor pluginBundleDescriptor) {
        for (GoPluginDescriptor pluginDescriptor : pluginBundleDescriptor.descriptors()) {
            for (PluginPostLoadHook pluginPostLoadHook : pluginPostLoadHooks) {
                final PluginPostLoadHook.Result result = pluginPostLoadHook.run(pluginDescriptor, pluginOSGiFramework.getExtensionsInfoFromThePlugin(pluginDescriptor.id()));
                if (result.isAFailure()) {
                    pluginBundleDescriptor.markAsInvalid(singletonList(result.getMessage()), null);
                    LOGGER.error(format("Skipped notifying all %s because of error: %s", PluginChangeListener.class.getSimpleName(), result.getMessage()));
                    return;
                }
            }
        }

        if (!pluginBundleDescriptor.isInvalid()) {
            IterableUtils.forEach(pluginBundleDescriptor.descriptors(), descriptor -> {
                IterableUtils.forEach(pluginChangeListeners, listener -> listener.pluginLoaded(descriptor));
            });
        }
    }

    private void handlePluginInvalidation(GoPluginBundleDescriptor bundleDescriptor, File bundleLocation) {
        String failureMsg = format("Failed to load plugin: %s. Plugin is invalid. Reasons %s",
                bundleLocation, bundleDescriptor.getMessages());
        LOGGER.error(failureMsg);
        unloadPlugin(bundleDescriptor);
    }
}
