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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.GoPluginApiMarker;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginHealthService;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Component
public class FelixGoPluginOSGiFramework implements GoPluginOSGiFramework {
    private static Logger LOGGER = LoggerFactory.getLogger(FelixGoPluginOSGiFramework.class);
    private final PluginRegistry registry;
    private Framework framework;
    private SystemEnvironment systemEnvironment;
    private Collection<PluginChangeListener> pluginChangeListeners = new ConcurrentLinkedQueue<>();
    private final PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator;

    @Autowired
    public FelixGoPluginOSGiFramework(PluginRegistry registry, SystemEnvironment systemEnvironment, PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator) {
        this.registry = registry;
        this.systemEnvironment = systemEnvironment;
        this.pluginExtensionsAndVersionValidator = pluginExtensionsAndVersionValidator;
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
    public Bundle loadPlugin(GoPluginDescriptor pluginDescriptor) {
        File bundleLocation = pluginDescriptor.bundleLocation();
        return getBundle(pluginDescriptor, bundleLocation);

    }

    private Bundle getBundle(GoPluginDescriptor pluginDescriptor, File bundleLocation) {
        Bundle bundle = null;
        try {
            bundle = framework.getBundleContext().installBundle("reference:" + bundleLocation.toURI());
            pluginDescriptor.setBundle(bundle);
            bundle.start();
            if (pluginDescriptor.isInvalid()) {
                handlePluginInvalidation(pluginDescriptor, bundleLocation, bundle);
                return bundle;
            }

            final PluginExtensionsAndVersionValidator.ValidationResult result = pluginExtensionsAndVersionValidator.validate(pluginDescriptor);

            if (result.hasError()) {
                pluginDescriptor.markAsInvalid(singletonList(result.toErrorMessage()), null);
                LOGGER.error(format("Skipped notifying all %s because of error: %s", PluginChangeListener.class.getSimpleName(), result.toErrorMessage()));
            } else {
                IterableUtils.forEach(pluginChangeListeners, notifyPluginLoadedEvent(pluginDescriptor));
            }

            return bundle;
        } catch (Exception e) {
            pluginDescriptor.markAsInvalid(asList(e.getMessage()), e);
            LOGGER.error("Failed to load plugin: {}", bundleLocation, e);
            handlePluginInvalidation(pluginDescriptor, bundleLocation, bundle);
            throw new RuntimeException("Failed to load plugin: " + bundleLocation, e);
        }
    }

    private void handlePluginInvalidation(GoPluginDescriptor pluginDescriptor, File bundleLocation, Bundle bundle) {
        String failureMsg = format("Failed to load plugin: %s. Plugin is invalid. Reasons %s",
                bundleLocation, pluginDescriptor.getStatus().getMessages());
        LOGGER.error(failureMsg);
        unloadPlugin(pluginDescriptor);
    }

    @Override
    public void unloadPlugin(GoPluginDescriptor pluginDescriptor) {
        Bundle bundle = pluginDescriptor.bundle();
        if (bundle == null) {
            return;
        }

        for (PluginChangeListener listener : pluginChangeListeners) {
            try {
                listener.pluginUnLoaded(pluginDescriptor);
            } catch (Exception e) {
                LOGGER.warn("A plugin unload listener ({}) failed: {}", listener.toString(), pluginDescriptor, e);
            }
        }

        try {
            bundle.stop();
            bundle.uninstall();
        } catch (Exception e) {
            throw new RuntimeException("Failed to unload plugin: " + bundle, e);
        }
    }

    @Override
    public void addPluginChangeListener(PluginChangeListener pluginChangeListener) {
        pluginChangeListeners.add(pluginChangeListener);
    }

    private void registerInternalServices(BundleContext bundleContext) {
        bundleContext.registerService(PluginHealthService.class, new DefaultPluginHealthService(registry), null);
        bundleContext.registerService(LoggingService.class, new DefaultPluginLoggingService(systemEnvironment), null);
    }

    Framework getFelixFramework(List<FrameworkFactory> frameworkFactories) {
        return frameworkFactories.get(0).newFramework(generateOSGiFrameworkConfig());
    }

    protected HashMap<String, String> generateOSGiFrameworkConfig() {
        String osgiFrameworkPackage = Bundle.class.getPackage().getName();
        String goPluginApiPackage = GoPluginApiMarker.class.getPackage().getName();
        String subPackagesOfGoPluginApiPackage = goPluginApiPackage + ".*";
        String internalServicesPackage = PluginHealthService.class.getPackage().getName();
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

    private <T> GoPluginDescriptor getDescriptorFor(ServiceReference<T> serviceReference) {
        String symbolicName = serviceReference.getBundle().getSymbolicName();
        return registry.getPlugin(symbolicName);
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, String extensionType, ActionWithReturn<T, R> action) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of {}", serviceReferenceClass);
            return null;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginIdAndExtensionType(serviceReferenceClass, pluginId, extensionType, bundleContext);
        ServiceReference<T> serviceReference = validateAndGetTheOnlyReferenceWithGivenSymbolicName(matchingServiceReferences, serviceReferenceClass, pluginId);
        T service = bundleContext.getService(serviceReference);
        return executeActionOnTheService(action, service, getDescriptorFor(serviceReference));
    }

    @Override
    public <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId, String extensionType) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of {}", serviceReferenceClass);
            return false;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginIdAndExtensionType(serviceReferenceClass, pluginId, extensionType, bundleContext);
        return !matchingServiceReferences.isEmpty();
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

    private <T> Collection<ServiceReference<T>> findServiceReferenceWithPluginIdAndExtensionType(Class<T> serviceReferenceClass, String pluginId, String extensionType, BundleContext bundleContext) {
        String filterBySymbolicNameAndExtensionType = format("(&(%s=%s)(%s=%s))", Constants.BUNDLE_SYMBOLICNAME, pluginId, Constants.BUNDLE_CATEGORY, extensionType);
        Collection<ServiceReference<T>> matchingServiceReferences;
        try {
            matchingServiceReferences = bundleContext.getServiceReferences(serviceReferenceClass, filterBySymbolicNameAndExtensionType);
        } catch (InvalidSyntaxException e) {
            String message = format("Failed to find reference for Service Reference %s and Filter %s", serviceReferenceClass, filterBySymbolicNameAndExtensionType);
            throw new GoPluginFrameworkException(message, e);
        }
        return matchingServiceReferences;
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

    private Closure<PluginChangeListener> notifyPluginLoadedEvent(final GoPluginDescriptor pluginDescriptor) {
        return o -> o.pluginLoaded(pluginDescriptor);
    }

}
