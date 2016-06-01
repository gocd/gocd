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

package com.thoughtworks.go.config.registry;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.plugins.PluginExtensions;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.PluggableViewModelFactory;
import org.jdom.Element;
import org.jdom.Namespace;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands mapping the implementers of a given Config interface.
 */
@Component
public class ConfigElementImplementationRegistry implements ConfigElementRegistry {

    private final ConcurrentMap<Class, List<Class>> registry;
    private final ConcurrentMap<String, PluginNamespace> xsdsFor;
    private final ConcurrentMap<Class, PluggableViewModelFactory> viewRegistry;

    @Autowired
    public ConfigElementImplementationRegistry(PluginExtensions pluginExtns) {
        this.registry = new ConcurrentHashMap<>();
        this.viewRegistry = new ConcurrentHashMap<>();
        this.xsdsFor = new ConcurrentHashMap<>();

        registerPluginExtensions(pluginExtns);
    }

    public <T> List<Class<? extends T>> implementersOf(Class<T> type) {
        List<Class<? extends T>> toReturn = new ArrayList<>();
        //noinspection unchecked
        for (Class<? extends T> impl : registry.get(type)) {
            toReturn.add(impl);
        }
        return toReturn;
    }

    public String xsds() {
        StringBuilder builder = new StringBuilder();
        for (PluginNamespace namespace : xsdsFor.values()) {
            builder.append(namespace.uri).append(" ").append(namespace.xsdResource).append(" ");
        }
        return builder.toString().trim();
    }

    public void xsdFor(BundleContext bundleContext, URL url) {
        registerPluginNamespace(new PluginNamespace(bundleContext, url));
    }

    public void registerNamespacesInto(Element element) {
        for (PluginNamespace namespace : xsdsFor.values()) {
            element.addNamespaceDeclaration(Namespace.getNamespace(namespace.prefix, namespace.uri));
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> PluggableViewModel getViewModelFor(T model, String actionName) {
        return viewRegistry.get(model.getClass()).viewModelFor(model, actionName);
    }

    private void registerPluginNamespace(PluginNamespace pluginNamespace) {
        this.xsdsFor.putIfAbsent(pluginNamespace.uri, pluginNamespace);
    }

    private void registerPluginExtensions(PluginExtensions pluginExtns) {
        for (ConfigurationExtension configExtension : pluginExtns.configTagImplementations()) {
            for (ConfigTypeExtension typeExtension : configExtension.implementations) {
                Class impl = typeExtension.getImplementation();
                //noinspection unchecked
                registerImplementer(typeExtension.getType(), impl);
                registerView(impl, typeExtension.getFactory());
            }
            registerPluginNamespace(configExtension.pluginNamespace);
        }
    }

    public <T> void registerImplementer(Class<T> configInterface, Class<? extends T> ...implementation) {
        List<Class> set;
        if (registry.containsKey(configInterface)) {
            set = registry.get(configInterface);
        } else {//TODO: concurrency issue -jj (someone sets set before putIfAbsent)
            List<Class> newSet = Collections.synchronizedList(new ArrayList<Class>());
            set = registry.putIfAbsent(configInterface, newSet);
            if (set == null) {
                set = newSet;
            }
        }
        set.addAll(Arrays.asList(implementation));
    }

    public void registerView(final Class klazz, final PluggableViewModelFactory factory) {
        viewRegistry.putIfAbsent(klazz, factory);
    }
}
