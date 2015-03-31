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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.commons.PluginUploadResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;

@Service
public class DefaultPluginManager implements PluginManager {
    private static final Logger LOGGER = Logger.getLogger(DefaultPluginManager.class);
    private final DefaultPluginJarLocationMonitor monitor;
    private DefaultPluginRegistry registry;
    private final DefaultPluginJarChangeListener listener;
    private GoApplicationAccessor goApplicationAccessor;
    private SystemEnvironment systemEnvironment;
    private File bundleLocation;
    private GoPluginOSGiFramework goPluginOSGiFramework;
    private PluginWriter pluginWriter;
    private PluginValidator pluginValidator;

    @Autowired
    public DefaultPluginManager(DefaultPluginJarLocationMonitor monitor, DefaultPluginRegistry registry, GoPluginOSGiFramework goPluginOSGiFramework,
                                DefaultPluginJarChangeListener listener, GoApplicationAccessor goApplicationAccessor, PluginWriter pluginWriter, PluginValidator pluginValidator, SystemEnvironment systemEnvironment) {
        this.monitor = monitor;
        this.registry = registry;
        this.listener = listener;
        this.goApplicationAccessor = goApplicationAccessor;
        this.systemEnvironment = systemEnvironment;
        bundleLocation = bundlePath();
        this.goPluginOSGiFramework = goPluginOSGiFramework;
        this.pluginWriter = pluginWriter;
        this.pluginValidator = pluginValidator;
    }

    @Override
    public List<GoPluginDescriptor> plugins() {
        return registry.plugins();
    }


    public PluginUploadResponse addPlugin(File uploadedPlugin, String filename) {
        if (!pluginValidator.namecheckForJar(filename)) {
            Map<Integer, String> errors = new HashMap<Integer, String>();
            errors.put(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, "Please upload a jar.");
            return PluginUploadResponse.create(false, null, errors);
        }
        return pluginWriter.addPlugin(uploadedPlugin, filename);
    }

    @Override
    public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
        return registry.getPlugin(pluginId);
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
        goPluginOSGiFramework.doOnAll(serviceReferenceClass, actionToDoOnEachRegisteredServiceWhichMatches);
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler) {
        goPluginOSGiFramework.doOnAllWithExceptionHandling(serviceReferenceClass, actionToDoOnEachRegisteredServiceWhichMatches, exceptionHandler);
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches) {
        return goPluginOSGiFramework.doOn(serviceReferenceClass, pluginId, actionToDoOnTheRegisteredServiceWhichMatches);
    }

    @Override
    public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        goPluginOSGiFramework.doOn(serviceReferenceClass, pluginId, action);
    }

    @Override
    public <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        if (goPluginOSGiFramework.hasReferenceFor(serviceReferenceClass, pluginId)) {
            doOn(serviceReferenceClass, pluginId, action);
        }
    }

    @Override
    public void startPluginInfrastructure() {
        if (!systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)) {
            return;
        }

        removeBundleDirectory();
        goPluginOSGiFramework.start();

        monitor.addPluginJarChangeListener(listener);
        monitor.start();
    }

    @Override
    public void stopPluginInfrastructure() {
        if (!systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)) {
            return;
        }

        goPluginOSGiFramework.stop();

        monitor.stop();
    }

    @Override
    public void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {
        PluginChangeListener filterChangeListener = new FilterChangeListener(goPluginOSGiFramework, pluginChangeListener, serviceReferenceClass);
        goPluginOSGiFramework.addPluginChangeListener(filterChangeListener);
    }

    @Override
    public GoPluginApiResponse submitTo(String pluginId, final GoPluginApiRequest apiRequest) {
        return goPluginOSGiFramework.doOn(GoPlugin.class, pluginId, new ActionWithReturn<GoPlugin, GoPluginApiResponse>() {
            @Override
            public GoPluginApiResponse execute(GoPlugin plugin, GoPluginDescriptor pluginDescriptor) {
                plugin.initializeGoApplicationAccessor(goApplicationAccessor);
                try {
                    return plugin.handle(apiRequest);
                } catch (UnhandledRequestTypeException e) {
                    LOGGER.error(e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public List<GoPluginIdentifier> allPluginsOfType(final String extension) {
        final List<GoPluginIdentifier> list = new ArrayList<GoPluginIdentifier>();
        goPluginOSGiFramework.doOnAll(GoPlugin.class, new Action<GoPlugin>() {
            @Override
            public void execute(GoPlugin plugin, GoPluginDescriptor pluginDescriptor) {
                GoPluginIdentifier goPluginIdentifier = plugin.pluginIdentifier();
                if (extension.equals(goPluginIdentifier.getExtension())) {
                    list.add(goPluginIdentifier);
                }
            }
        });
        return list;
    }

    @Override
    public boolean hasReferenceFor(Class serviceReferenceClass, String pluginId) {
        return goPluginOSGiFramework.hasReferenceFor(serviceReferenceClass, pluginId);
    }

    @Override
    public boolean isPluginOfType(final String extension, String pluginId) {
        return hasReferenceFor(GoPlugin.class, pluginId) && goPluginOSGiFramework.doOn(GoPlugin.class, pluginId, new ActionWithReturn<GoPlugin, Boolean>() {
            @Override
            public Boolean execute(GoPlugin plugin, GoPluginDescriptor pluginDescriptor) {
                return extension.equals(plugin.pluginIdentifier().getExtension());
            }
        });
    }

    @Override
    public String resolveExtensionVersion(String pluginId, final List<String> goSupportedExtensionVersions) {
        String resolvedExtensionVersion = doOn(GoPlugin.class, pluginId, new ActionWithReturn<GoPlugin, String>() {
            @Override
            public String execute(GoPlugin goPlugin, GoPluginDescriptor pluginDescriptor) {
                List<String> pluginSupportedVersions = goPlugin.pluginIdentifier().getSupportedExtensionVersions();
                String currentMaxVersion = "0";
                for (String pluginSupportedVersion : pluginSupportedVersions) {
                    if (goSupportedExtensionVersions.contains(pluginSupportedVersion) && parseDouble(currentMaxVersion) < parseDouble(pluginSupportedVersion)) {
                        currentMaxVersion = pluginSupportedVersion;
                    }
                }
                return currentMaxVersion;
            }
        });
        if ("0".equals(resolvedExtensionVersion)) {
            throw new RuntimeException(String.format("Could not find matching extension version between Plugin[%s] and Go", pluginId));
        }
        return resolvedExtensionVersion;
    }

    public String loadPluginSettings(String pluginId) {
        GoPluginApiResponse response;
        try {
            response = submitTo(pluginId, new DefaultGoPluginApiRequest("", "", "load-settings"));
        } catch (Exception e) {
            LOGGER.warn(format("Could not load settings for Plugin %s", pluginId), e);
            throw new RuntimeException(format("Could not load settings for Plugin %s. %s", pluginId, e.getMessage()));
        }
        if (SUCCESS_RESPONSE_CODE == response.responseCode()) {
            return response.responseBody();
        }
        throw new RuntimeException(format("Error while loading settings for plugin %s. [%s]", pluginId, response.responseBody()));
    }

    public void savePluginSettings(String pluginId, String settingsAsJson) {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("", "", "save-settings");
        request.setRequestBody(settingsAsJson);
        GoPluginApiResponse response;
        try {
            response = submitTo(pluginId, request);
        } catch (Exception e) {
            LOGGER.warn(format("Could not save settings for Plugin %s. %s", pluginId, e.getMessage()), e);
            throw new RuntimeException(format("Could not save settings for Plugin %s. %s", pluginId, e.getMessage()));
        }
        if (SUCCESS_RESPONSE_CODE != response.responseCode()) {
            throw new RuntimeException(format("Error while saving settings for plugin %s. [%s]", pluginId, response.responseBody()));
        }
    }

    private void removeBundleDirectory() {
        try {
            FileUtils.deleteDirectory(bundleLocation);
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to copy delete bundle directory %s", bundleLocation), e);
        }
    }

    private File bundlePath() {
        File bundleDir = new File(systemEnvironment.get(PLUGIN_BUNDLE_PATH));
        FileUtil.validateAndCreateDirectory(bundleDir);
        return bundleDir;
    }

    private static class FilterChangeListener implements PluginChangeListener {
        private final GoPluginOSGiFramework goPluginOSGiFramework;
        private final PluginChangeListener pluginChangeListenerDelegate;
        private final Class<?>[] serviceReferences;

        public FilterChangeListener(GoPluginOSGiFramework goPluginOSGiFramework,
                                    PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {
            this.goPluginOSGiFramework = goPluginOSGiFramework;
            pluginChangeListenerDelegate = pluginChangeListener;
            serviceReferences = serviceReferenceClass;
        }

        @Override
        public void pluginLoaded(GoPluginDescriptor descriptor) {
            String pluginId = descriptor.id();
            if (shouldCallDelegate(pluginId)) {
                pluginChangeListenerDelegate.pluginLoaded(descriptor);
            }
        }

        private boolean shouldCallDelegate(String pluginId) {
            for (Class<?> serviceReference : serviceReferences) {
                if (goPluginOSGiFramework.hasReferenceFor(serviceReference, pluginId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void pluginUnLoaded(GoPluginDescriptor descriptor) {
            pluginChangeListenerDelegate.pluginUnLoaded(descriptor);
        }
    }
}
