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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;

@Service
public class DefaultPluginManager implements PluginManager {
    private static final Logger LOGGER = Logger.getLogger(DefaultPluginManager.class);
    private final DefaultPluginJarLocationMonitor monitor;
    private DefaultPluginRegistry registry;
    private final DefaultPluginJarChangeListener listener;
    private SystemEnvironment systemEnvironment;
    private File bundleLocation;
    private GoPluginOSGiFramework goPluginOSGiFramework;

    @Autowired
    public DefaultPluginManager(DefaultPluginJarLocationMonitor monitor, DefaultPluginRegistry registry, GoPluginOSGiFramework goPluginOSGiFramework,
                                DefaultPluginJarChangeListener listener, SystemEnvironment systemEnvironment) {
        this.monitor = monitor;
        this.registry = registry;
        this.listener = listener;
        this.systemEnvironment = systemEnvironment;
        bundleLocation = bundlePath();
        this.goPluginOSGiFramework = goPluginOSGiFramework;
    }

    @Override
    public List<GoPluginDescriptor> plugins() {
        return registry.plugins();
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
        if(goPluginOSGiFramework.hasReferenceFor(serviceReferenceClass, pluginId)){
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
        PluginChangeListener filterChangeListener=new FilterChangeListener(goPluginOSGiFramework,pluginChangeListener,serviceReferenceClass);
        goPluginOSGiFramework.addPluginChangeListener(filterChangeListener);
    }

    private void removeBundleDirectory() {
        try {
            FileUtils.deleteDirectory(bundleLocation);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy delete bundle directory %s", bundleLocation), e);
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
                if(goPluginOSGiFramework.hasReferenceFor(serviceReference, pluginId)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public void pluginUnLoaded(GoPluginDescriptor descriptor) {
            String pluginId = descriptor.id();
            if (shouldCallDelegate(pluginId)) {
                pluginChangeListenerDelegate.pluginUnLoaded(descriptor);
            }
        }
    }
}
