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

import com.thoughtworks.go.plugin.api.GoPluginApiMarker;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginHealthService;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.IteratorUtils;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.log4j.Logger;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.forAllDo;

@Component
public class FelixGoPluginOSGiFramework implements GoPluginOSGiFramework {
    private static Logger LOGGER = Logger.getLogger(FelixGoPluginOSGiFramework.class);
    private final PluginRegistry registry;
    private Framework framework;
    private SystemEnvironment systemEnvironment;
    private Collection<PluginChangeListener> pluginChangeListeners = new ConcurrentLinkedQueue<>();

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
        } catch (BundleException e) {
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
        Bundle bundle =null;
        try {
            bundle = framework.getBundleContext().installBundle("reference:" + bundleLocation.toURI());
            pluginDescriptor.setBundle(bundle);
            bundle.start();
            if(pluginDescriptor.isInvalid()){
                handlePluginInvalidation(pluginDescriptor, bundleLocation, bundle);
                return bundle;
            }
            forAllDo(pluginChangeListeners, notifyPluginLoadedEvent(pluginDescriptor));
            return bundle;
        } catch (Exception e) {
            pluginDescriptor.markAsInvalid(asList(e.getMessage()), e);
            LOGGER.error("Failed to load plugin: " + bundleLocation,e);
            stopAndUninstallBundle(bundle, bundleLocation);
            throw new RuntimeException("Failed to load plugin: " + bundleLocation, e);
        }
    }

    private void handlePluginInvalidation(GoPluginDescriptor pluginDescriptor, File bundleLocation, Bundle bundle) {
        String failureMsg = String.format("Failed to load plugin: %s. Plugin is invalid. Reasons %s",
                bundleLocation, pluginDescriptor.getStatus().getMessages());
        LOGGER.error(failureMsg);
        stopAndUninstallBundle(bundle, bundleLocation);
    }

    private void stopAndUninstallBundle(Bundle bundle, File bundleLocation) {
        if(bundle!=null){
            try {
                bundle.stop();
                bundle.uninstall();
            } catch (BundleException e) {
                String stopFailMsg = "Failed to stop/uninstall bundle: " + bundleLocation;
                LOGGER.error(stopFailMsg,e);
                throw new RuntimeException(stopFailMsg, e);
            }
        }
    }

    @Override
    public void unloadPlugin(GoPluginDescriptor pluginDescriptor) {
        Bundle bundle = pluginDescriptor.bundle();
        if (bundle == null) {
            return;
        }

        try {
            bundle.stop();
            bundle.uninstall();
            forAllDo(pluginChangeListeners, notifyPluginUnLoadedEvent(pluginDescriptor));
        } catch (BundleException e) {
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

    private HashMap<String, String> generateOSGiFrameworkConfig() {
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

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
        doOnAllWithExceptionHandling(serviceReferenceClass, actionToDoOnEachRegisteredServiceWhichMatches, new ExceptionHandler<T>() {
            @Override
            public void handleException(T obj, Throwable t) {
                throw new RuntimeException(t.getMessage(), t);
            }
        });
    }

    @Override
    public <T> void doOnAllWithExceptionHandling(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> handler) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of " + serviceReferenceClass);
            return;
        }
        BundleContext bundleContext = framework.getBundleContext();


        Collection<ServiceReference<T>> matchingServiceReferences;
        try {
            matchingServiceReferences = bundleContext.getServiceReferences(serviceReferenceClass, null);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }

        for (ServiceReference<T> currentServiceReference : matchingServiceReferences) {

            T service = bundleContext.getService(currentServiceReference);
            GoPluginDescriptor descriptor = getDescriptorFor(currentServiceReference);
            try {

                actionToDoOnEachRegisteredServiceWhichMatches.execute(service, descriptor);
            } catch (Throwable t) {
                handler.handleException(service, t);
            }
        }
    }

    private <T> GoPluginDescriptor getDescriptorFor(ServiceReference<T> serviceReference) {
        String symbolicName = serviceReference.getBundle().getSymbolicName();
        return registry.getPlugin(symbolicName);
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> action) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of " + serviceReferenceClass);
            return null;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginId(serviceReferenceClass, pluginId, bundleContext);
        ServiceReference<T> serviceReference = validateAndGetTheOnlyReferenceWithGivenSymbolicName(matchingServiceReferences, serviceReferenceClass, pluginId);
        T service = bundleContext.getService(serviceReference);
        return executeActionOnTheService(action, service, getDescriptorFor(serviceReference));
    }

    @Override
    public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        doOnWithExceptionHandling(serviceReferenceClass, pluginId, action, null);
    }

    @Override
    public <T> void doOnWithExceptionHandling(Class<T> serviceReferenceClass, String pluginId, Action<T> action, ExceptionHandler<T> handler) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of " + serviceReferenceClass);
            return;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginId(serviceReferenceClass, pluginId, bundleContext);
        ServiceReference<T> serviceReference = validateAndGetTheOnlyReferenceWithGivenSymbolicName(matchingServiceReferences, serviceReferenceClass, pluginId);
        T service = bundleContext.getService(serviceReference);
        executeActionOnTheService(action, service, getDescriptorFor(serviceReference), handler);

    }

    @Override
    public <T> void doOnAllForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        doOnAllWithExceptionHandlingForPlugin(serviceReferenceClass, pluginId, action, null);
    }

    @Override
    public <T> void doOnAllWithExceptionHandlingForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action,
                                                          ExceptionHandler<T> handler) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of " + serviceReferenceClass);
            return;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginId(serviceReferenceClass, pluginId, bundleContext);
        for (ServiceReference<T> serviceReference : matchingServiceReferences) {
            T service = bundleContext.getService(serviceReference);
            executeActionOnTheService(action, service, getDescriptorFor(serviceReference), handler);
        }
    }

    @Override
    public <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId) {
        if (framework == null) {
            LOGGER.warn("[Plugin Framework] Plugins are not enabled, so cannot do an action on all implementations of " + serviceReferenceClass);
            return false;
        }

        BundleContext bundleContext = framework.getBundleContext();
        Collection<ServiceReference<T>> matchingServiceReferences = findServiceReferenceWithPluginId(serviceReferenceClass, pluginId, bundleContext);
        return !matchingServiceReferences.isEmpty();
    }

    private <T> void executeActionOnTheService(Action<T> action, T service, GoPluginDescriptor goPluginDescriptor, ExceptionHandler<T> handler) {
        try {
            action.execute(service, goPluginDescriptor);
        } catch (Throwable t) {
            if (handler != null) {
                handler.handleException(service, t);
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }
    }

    private <T, R> R executeActionOnTheService(ActionWithReturn<T, R> action, T service, GoPluginDescriptor goPluginDescriptor) {
        try {
            return action.execute(service, goPluginDescriptor);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    private <T> Collection<ServiceReference<T>> findServiceReferenceWithPluginId(Class<T> serviceReferenceClass, String pluginId, BundleContext bundleContext) {
        String filterBySymbolicName = String.format("(%s=%s)", Constants.BUNDLE_SYMBOLICNAME, pluginId);
        Collection<ServiceReference<T>> matchingServiceReferences;
        try {
            matchingServiceReferences = bundleContext.getServiceReferences(serviceReferenceClass, filterBySymbolicName);
        } catch (InvalidSyntaxException e) {
            String message = String.format("Failed To find reference for Service Reference %s and Filter %s", serviceReferenceClass, filterBySymbolicName);
            throw new GoPluginFrameworkException(message, e);
        }
        return matchingServiceReferences;
    }

    private <T> ServiceReference<T> validateAndGetTheOnlyReferenceWithGivenSymbolicName(Collection<ServiceReference<T>> matchingServiceReferences,
                                                                                        Class<T> serviceReference, String pluginId) {
        if (matchingServiceReferences.isEmpty()) {
            throw new GoPluginFrameworkException(String.format("No reference found for the given Service Reference: %s and Plugin Id %s. It is likely that the plugin is missing.",
                    serviceReference.getCanonicalName(), pluginId));
        }

        if (matchingServiceReferences.size() > 1) {
            throw new GoPluginFrameworkException(String.format("More than one reference found for the given "
                    + "Service Reference: %s and Plugin Id %s; References: %s", serviceReference.getCanonicalName(), pluginId, matchingServiceReferences));
        }

        return matchingServiceReferences.iterator().next();
    }

    private Closure notifyPluginLoadedEvent(final GoPluginDescriptor pluginDescriptor) {
        return new Closure() {
            @Override
            public void execute(Object o) {
                PluginChangeListener pluginChangeListener = (PluginChangeListener) o;
                pluginChangeListener.pluginLoaded(pluginDescriptor);
            }
        };
    }

    private Closure notifyPluginUnLoadedEvent(final GoPluginDescriptor pluginDescriptor) {
        return new Closure() {
            @Override
            public void execute(Object o) {
                PluginChangeListener pluginChangeListener = (PluginChangeListener) o;
                pluginChangeListener.pluginUnLoaded(pluginDescriptor);
            }
        };
    }

}
