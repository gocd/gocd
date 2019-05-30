/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import static java.util.Arrays.asList;
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
        pluginOSGiFramework.addPluginChangeListener(pluginChangeListener);
        pluginChangeListeners.add(pluginChangeListener);
    }

    public PluginPostLoadHook addPluginPostLoadHook(PluginPostLoadHook pluginPostLoadHook) {
        pluginPostLoadHooks.add(pluginPostLoadHook);
        return pluginOSGiFramework.addPostLoadHook(pluginPostLoadHook);
    }

    public void loadPlugin(GoPluginDescriptor descriptor) {
        pluginOSGiFramework.loadPlugin(descriptor);

        if (!descriptor.isInvalid()) {
            doPostBundleInstallActivities(descriptor, descriptor.bundleLocation());
        }
    }

    public void unloadPlugin(GoPluginDescriptor descriptorOfRemovedPlugin) {
        pluginOSGiFramework.unloadPlugin(descriptorOfRemovedPlugin);
    }

    private void doPostBundleInstallActivities(GoPluginDescriptor pluginDescriptor, File bundleLocation) {
        try {
            for (PluginPostLoadHook pluginPostLoadHook : pluginPostLoadHooks) {
                final PluginPostLoadHook.Result result = pluginPostLoadHook.run(pluginDescriptor, pluginOSGiFramework.getExtensionsInfoFromThePlugin(pluginDescriptor.id()));
                if (result.isAFailure()) {
                    pluginDescriptor.markAsInvalid(singletonList(result.getMessage()), null);
                    LOGGER.error(format("Skipped notifying all %s because of error: %s", PluginChangeListener.class.getSimpleName(), result.getMessage()));
                    return;
                }
            }

            if (!pluginDescriptor.isInvalid()) {
                IterableUtils.forEach(pluginChangeListeners, listener -> listener.pluginLoaded(pluginDescriptor));
            }
        } catch (Exception e) {
            pluginDescriptor.markAsInvalid(asList(e.getMessage()), e);
            LOGGER.error("Failed to load plugin: {}", bundleLocation, e);
            handlePluginInvalidation(pluginDescriptor, bundleLocation);
            throw new RuntimeException("Failed to load plugin: " + bundleLocation, e);
        }
    }

    private void handlePluginInvalidation(GoPluginDescriptor pluginDescriptor, File bundleLocation) {
        String failureMsg = format("Failed to load plugin: %s. Plugin is invalid. Reasons %s",
                bundleLocation, pluginDescriptor.getStatus().getMessages());
        LOGGER.error(failureMsg);
        unloadPlugin(pluginDescriptor);
    }
}
