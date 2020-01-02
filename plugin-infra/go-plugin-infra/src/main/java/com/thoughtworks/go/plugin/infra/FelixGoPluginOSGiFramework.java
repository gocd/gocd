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
import com.thoughtworks.go.plugin.api.GoPluginApiMarker;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginRegistryService;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginRegistryService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.keyvalue.AbstractKeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

@Component
public class FelixGoPluginOSGiFramework implements GoPluginOSGiFramework {
    private static Logger LOGGER = LoggerFactory.getLogger(FelixGoPluginOSGiFramework.class);
    private final PluginRegistry registry;
    private Framework framework;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public FelixGoPluginOSGiFramework(PluginRegistry registry, SystemEnvironment systemEnvironment) {
        this.registry = registry;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void start() {
        List<FrameworkFactory> frameworkFactories = IteratorUtils.toList(ServiceLoader.load(FrameworkFactory.class).iterator());

        if (frameworkFactories.size() != 1) {
            throw new RuntimeException("One OSGi framework expected. Got " + frameworkFactories.size() + ": " + frameworkFactories);
        }

        try {
            framework = getFelixFramework(frameworkFactories);
            framework.start();
            registerInternalServices(framework.getBundleContext());
        } catch (BundleException e) {
            throw new RuntimeException("Failed to initialize OSGi framework", e);
        }
    }

    @Override
    public void stop() {
        if (framework == null) {
            return;
        }

        try {
            framework.stop();
            framework.waitForStop(10000L);
        } catch (BundleException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        registry.clear();
    }

    @Override
    public Bundle loadPlugin(GoPluginBundleDescriptor pluginBundleDescriptor) {
        File bundleLocation = pluginBundleDescriptor.bundleLocation();
        try {
            Bundle bundle = framework.getBundleContext().installBundle("reference:" + bundleLocation.toURI());
            pluginBundleDescriptor.setBundle(bundle);
            bundle.start();
            return bundle;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unloadPlugin(GoPluginBundleDescriptor pluginDescriptor) {
        Bundle bundle = pluginDescriptor.bundle();

        if (bundle == null || bundle.getState() == Bundle.UNINSTALLED) {
            LOGGER.info(format("Skipping plugin '%s' uninstall as it is already uninstalled.", pluginDescriptor));
            return;
        }

        try {
            bundle.stop();
            bundle.uninstall();
        } catch (Exception e) {
            throw new RuntimeException("Failed to unload plugin: " + bundle, e);
        }
    }

    private void registerInternalServices(BundleContext bundleContext) {
        bundleContext.registerService(PluginRegistryService.class, new DefaultPluginRegistryService(registry), null);
        bundleContext.registerService(LoggingService.class, new DefaultPluginLoggingService(systemEnvironment), null);
    }

    Framework getFelixFramework(List<FrameworkFactory> frameworkFactories) {
        return frameworkFactories.get(0).newFramework(generateOSGiFrameworkConfig());
    }

    protected HashMap<String, String> generateOSGiFrameworkConfig() {
        String osgiFrameworkPackage = Bundle.class.getPackage().getName();
        String goPluginApiPackage = GoPluginApiMarker.class.getPackage().getName();
        String subPackagesOfGoPluginApiPackage = goPluginApiPackage + ".*";
        String internalServicesPackage = PluginRegistryService.class.getPackage().getName();
        String javaxPackages = "javax.*";
        String orgXmlSaxPackages = "org.xml.sax, org.xml.sax.*";
        String orgW3cDomPackages = "org.w3c.dom, org.w3c.dom.*";

        HashMap<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
        config.put(Constants.FRAMEWORK_BOOTDELEGATION, osgiFrameworkPackage + ", " + goPluginApiPackage + ", " + subPackagesOfGoPluginApiPackage
                + ", " + internalServicesPackage + ", " + javaxPackages + ", " + orgXmlSaxPackages + ", " + orgW3cDomPackages);
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "onFirstInit");
        config.put(BundleCache.CACHE_LOCKING_PROP, "false");
        config.put(FelixConstants.SERVICE_URLHANDLERS_PROP, "false");
        return config;
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, String extensionType, ActionWithReturn<T, R> action) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of {}", serviceReferenceClass);
            return null;
        }

        BundleContext bundleContext = framework.getBundleContext();
        ServiceQuery serviceQuery = ServiceQuery.newQuery(pluginId).withExtension(extensionType);

        Collection<ServiceReference<T>> matchingServiceReferences = listServices(bundleContext, serviceReferenceClass, serviceQuery);
        ServiceReference<T> serviceReference = validateAndGetTheOnlyReferenceWithGivenSymbolicName(matchingServiceReferences, serviceReferenceClass, pluginId);
        T service = bundleContext.getService(serviceReference);
        return executeActionOnTheService(action, service, registry.getPlugin(pluginId));
    }

    @Override
    public <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId, String extensionType) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of {}", serviceReferenceClass);
            return false;
        }

        BundleContext bundleContext = framework.getBundleContext();
        ServiceQuery serviceQuery = ServiceQuery.newQuery(pluginId).withExtension(extensionType);
        Collection<ServiceReference<T>> matchingServiceReferences = listServices(bundleContext, serviceReferenceClass, serviceQuery);
        return !matchingServiceReferences.isEmpty();
    }

    @Override
    public Map<String, List<String>> getExtensionsInfoFromThePlugin(String pluginId) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of {}", GoPlugin.class);
            return null;
        }

        final BundleContext bundleContext = framework.getBundleContext();
        final ServiceQuery serviceQuery = ServiceQuery.newQuery(pluginId);
        final Collection<ServiceReference<GoPlugin>> serviceReferences = new HashSet<>(listServices(bundleContext, GoPlugin.class, serviceQuery));

        ActionWithReturn<GoPlugin, DefaultKeyValue<String, List<String>>> action = (goPlugin, descriptor) -> new DefaultKeyValue<>(goPlugin.pluginIdentifier().getExtension(), goPlugin.pluginIdentifier().getSupportedExtensionVersions());

        return serviceReferences.stream()
                .map(serviceReference -> {
                    GoPlugin service = bundleContext.getService(serviceReference);
                    return executeActionOnTheService(action, service, registry.getPlugin(pluginId));
                }).collect(toMap(AbstractKeyValue::getKey, AbstractKeyValue::getValue));
    }

    private <T, R> R executeActionOnTheService(ActionWithReturn<T, R> action, T service, GoPluginDescriptor goPluginDescriptor) {
        try {
            if (systemEnvironment.pluginClassLoaderHasOldBehaviour()) {
                return action.execute(service, goPluginDescriptor);
            } else {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(service.getClass().getClassLoader());
                try {
                    return action.execute(service, goPluginDescriptor);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    private <T> Collection<ServiceReference<T>> listServices(BundleContext bundleContext, Class<T> serviceReferenceClass, ServiceQuery serviceQuery) {
        try {
            return bundleContext.getServiceReferences(serviceReferenceClass, serviceQuery.build());
        } catch (InvalidSyntaxException e) {
            String message = format("Failed to find reference for Service Reference %s and Filter %s", serviceReferenceClass, serviceQuery.build());
            throw new GoPluginFrameworkException(message, e);
        }
    }

    private <T> ServiceReference<T> validateAndGetTheOnlyReferenceWithGivenSymbolicName(Collection<ServiceReference<T>> matchingServiceReferences,
                                                                                        Class<T> serviceReference, String pluginId) {
        if (matchingServiceReferences.isEmpty()) {
            throw new GoPluginFrameworkException(format("No reference found for the given Service Reference: %s and Plugin Id %s. It is likely that the plugin is missing.",
                    serviceReference.getCanonicalName(), pluginId));
        }

        if (matchingServiceReferences.size() > 1) {
            throw new GoPluginFrameworkException(format("More than one reference found for the given "
                    + "Service Reference: %s and Plugin Id %s; References: %s", serviceReference.getCanonicalName(), pluginId, matchingServiceReferences));
        }

        return matchingServiceReferences.iterator().next();
    }
}
