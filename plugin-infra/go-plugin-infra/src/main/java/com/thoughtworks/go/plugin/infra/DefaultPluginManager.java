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

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_WORK_DIR;
import static java.lang.Double.parseDouble;

@Service
public class DefaultPluginManager implements PluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginManager.class);
    private final DefaultPluginJarLocationMonitor monitor;
    private DefaultPluginRegistry registry;
    private final DefaultPluginJarChangeListener defaultPluginJarChangeListener;
    private SystemEnvironment systemEnvironment;
    private PluginLoader pluginLoader;
    private File bundleLocation;
    private GoPluginOSGiFramework goPluginOSGiFramework;
    private final Map<PluginDescriptor, Set<String>> initializedPluginsWithTheirExtensionTypes = new HashMap<>();
    private PluginRequestProcessorRegistry requestProcessRegistry;

    @Autowired
    public DefaultPluginManager(DefaultPluginJarLocationMonitor monitor, DefaultPluginRegistry registry, GoPluginOSGiFramework goPluginOSGiFramework,
                                DefaultPluginJarChangeListener defaultPluginJarChangeListener, PluginRequestProcessorRegistry requestProcessRegistry,
                                SystemEnvironment systemEnvironment, PluginLoader pluginLoader) {
        this.monitor = monitor;
        this.registry = registry;
        this.defaultPluginJarChangeListener = defaultPluginJarChangeListener;
        this.requestProcessRegistry = requestProcessRegistry;
        this.systemEnvironment = systemEnvironment;
        this.pluginLoader = pluginLoader;
        bundleLocation = bundlePath();
        this.goPluginOSGiFramework = goPluginOSGiFramework;
    }

    @Override
    public List<GoPluginDescriptor> plugins() {
        return registry.plugins();
    }

    @Override
    public boolean isPluginLoaded(String pluginId) {
        final GoPluginDescriptor descriptor = getPluginDescriptorFor(pluginId);
        return !(descriptor == null || descriptor.isInvalid());
    }

    @Override
    public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
        return registry.getPlugin(pluginId);
    }

    @Override
    public void startInfrastructure(boolean shouldPoll) {
        removeBundleDirectory();
        goPluginOSGiFramework.start();

        addPluginChangeListener(new PluginChangeListener() {
            @Override
            public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {

            }

            @Override
            public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
                synchronized (initializedPluginsWithTheirExtensionTypes) {
                    initializedPluginsWithTheirExtensionTypes.remove(pluginDescriptor);
                }
            }
        });

        monitor.addPluginJarChangeListener(defaultPluginJarChangeListener);
        if (shouldPoll) {
            monitor.start();
        } else {
            monitor.oneShot();
        }
    }

    @Override
    public void stopInfrastructure() {
        goPluginOSGiFramework.stop();
        monitor.stop();
        initializedPluginsWithTheirExtensionTypes.clear();
    }

    @Override
    public void addPluginChangeListener(PluginChangeListener pluginChangeListener) {
        pluginLoader.addPluginChangeListener(pluginChangeListener);
    }

    @Override
    public PluginPostLoadHook addPluginPostLoadHook(PluginPostLoadHook pluginPostLoadHook) {
        return pluginLoader.addPluginPostLoadHook(pluginPostLoadHook);
    }

    @Override
    public GoPluginApiResponse submitTo(final String pluginId, String extensionType, final GoPluginApiRequest apiRequest) {
        return goPluginOSGiFramework.doOn(GoPlugin.class, pluginId, extensionType, (plugin, pluginDescriptor) -> {
            ensureInitializerInvoked(pluginDescriptor, plugin, extensionType);
            try {
                return plugin.handle(apiRequest);
            } catch (UnhandledRequestTypeException e) {
                LOGGER.error(e.getMessage());
                LOGGER.debug(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private void ensureInitializerInvoked(GoPluginDescriptor pluginDescriptor, GoPlugin plugin, String extensionType) {
        synchronized (initializedPluginsWithTheirExtensionTypes) {
            if (initializedPluginsWithTheirExtensionTypes.get(pluginDescriptor) == null) {
                initializedPluginsWithTheirExtensionTypes.put(pluginDescriptor, new HashSet<>());
            }

            Set<String> initializedExtensions = initializedPluginsWithTheirExtensionTypes.get(pluginDescriptor);
            if (initializedExtensions == null || initializedExtensions.contains(extensionType)) {
                return;
            }
            initializedPluginsWithTheirExtensionTypes.get(pluginDescriptor).add(extensionType);

            PluginAwareDefaultGoApplicationAccessor accessor = new PluginAwareDefaultGoApplicationAccessor(pluginDescriptor, requestProcessRegistry);
            plugin.initializeGoApplicationAccessor(accessor);
        }
    }

    @Override
    public boolean isPluginOfType(final String extension, String pluginId) {
        return goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginId, extension);
    }

    @Override
    public String resolveExtensionVersion(String pluginId, String extensionType, final List<String> goSupportedExtensionVersions) {
        List<String> pluginSupportedVersions = getRequiredExtensionVersionsByPlugin(pluginId, extensionType);
        String resolvedExtensionVersion = "0";
        for (String pluginSupportedVersion : pluginSupportedVersions) {
            if (goSupportedExtensionVersions.contains(pluginSupportedVersion) && parseDouble(resolvedExtensionVersion) < parseDouble(pluginSupportedVersion)) {
                resolvedExtensionVersion = pluginSupportedVersion;
            }
        }

        if ("0".equals(resolvedExtensionVersion)) {
            throw new RuntimeException(String.format("Could not find matching extension version between Plugin[%s] and Go", pluginId));
        }

        return resolvedExtensionVersion;
    }

    @Override
    public List<String> getRequiredExtensionVersionsByPlugin(String pluginId, String extensionType) {
        return goPluginOSGiFramework.doOn(GoPlugin.class, pluginId, extensionType, (goPlugin, pluginDescriptor) -> goPlugin.pluginIdentifier().getSupportedExtensionVersions());
    }

    private void removeBundleDirectory() {
        try {
            FileUtils.deleteDirectory(bundleLocation);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy delete bundle directory %s", bundleLocation), e);
        }
    }

    private File bundlePath() {
        File bundleDir = new File(systemEnvironment.get(PLUGIN_WORK_DIR));
        FileUtil.validateAndCreateDirectory(bundleDir);
        return bundleDir;
    }
}
