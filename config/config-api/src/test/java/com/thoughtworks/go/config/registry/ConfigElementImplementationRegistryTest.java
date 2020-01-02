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
package com.thoughtworks.go.config.registry;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.config.BuildTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.plugins.PluginExtensions;
import com.thoughtworks.go.plugins.PluginTestUtil;
import com.thoughtworks.go.plugins.presentation.PluggableViewModelFactory;
import com.thoughtworks.go.presentation.TaskViewModel;
import com.thoughtworks.go.util.DataStructureUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigElementImplementationRegistryTest {
    private PluginExtensions pluginExtns;

    @Before
    public void setUp() {
        pluginExtns = mock(PluginExtensions.class);
        List<ConfigurationExtension> configTags = new ArrayList<>();
        when(pluginExtns.configTagImplementations()).thenReturn(configTags);
    }

    @Test
    public void shouldProvideTheNamespaceUriAndTheLocation() throws MalformedURLException {
        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(pluginExtns);

        URL resource = new File("file:///tmp/foo").toURI().toURL();
        URL resource1 = new File("file:///tmp/bar").toURI().toURL();
        registry.xsdFor(PluginTestUtil.bundleCtxWithHeaders(m(PluginNamespace.XSD_NAMESPACE_PREFIX, "uri-1", PluginNamespace.XSD_NAMESPACE_URI, "uri1")), resource);
        registry.xsdFor(PluginTestUtil.bundleCtxWithHeaders(m(PluginNamespace.XSD_NAMESPACE_PREFIX, "uri-2", PluginNamespace.XSD_NAMESPACE_URI, "uri2")), resource);
        registry.xsdFor(PluginTestUtil.bundleCtxWithHeaders(m(PluginNamespace.XSD_NAMESPACE_PREFIX, "uri-3", PluginNamespace.XSD_NAMESPACE_URI, "uri3")), resource1);

        assertThat(registry.xsds(), containsString(String.format("uri1 %s", resource.toString())));
        assertThat(registry.xsds(), containsString(String.format("uri2 %s", resource.toString())));
        assertThat(registry.xsds(), containsString(String.format("uri3 %s", resource1.toString())));
    }

    @Test
    public void shouldAddPluginNamespaceToPassedInElement() throws MalformedURLException {
        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(pluginExtns);
        registry.xsdFor(PluginTestUtil.bundleCtxWithHeaders(m(PluginNamespace.XSD_NAMESPACE_PREFIX, "something", PluginNamespace.XSD_NAMESPACE_URI, "uri")), new File("file:///tmp/foo").toURI().toURL());
        registry.xsdFor(PluginTestUtil.bundleCtxWithHeaders(m(PluginNamespace.XSD_NAMESPACE_PREFIX, "second", PluginNamespace.XSD_NAMESPACE_URI, "uri-1")), new File("file:///tmp/foo1").toURI().toURL());
        Element foo = new Element("foo");
        registry.registerNamespacesInto(foo);
        assertThat(foo.getNamespace("something"), is(Namespace.getNamespace("something", "uri")));
        assertThat(foo.getNamespace("second"), is(Namespace.getNamespace("second", "uri-1")));
    }

    private static final class TestTaskConfigTypeExtension<T extends Task> implements ConfigTypeExtension<Task> {
        private Class<T> implType;
        private PluggableViewModelFactory<T> factory;

        private TestTaskConfigTypeExtension(Class<T> implType, PluggableViewModelFactory<T> factory) {
            this.implType = implType;
            this.factory = factory;
        }

        @Override
        public Class<Task> getType() {
            return Task.class;
        }

        @Override
        public Class<? extends Task> getImplementation() {
            return implType;
        }

        @Override
        public PluggableViewModelFactory<? extends Task> getFactory() {
            return factory;
        }
    }

    @Test
    public void shouldCreateTaskViewModelForPlugins() throws MalformedURLException {
        BundleContext execCtx = PluginTestUtil.bundleCtxWithHeaders(DataStructureUtils.m(PluginNamespace.XSD_NAMESPACE_PREFIX, "exec", PluginNamespace.XSD_NAMESPACE_URI, "uri-exec"));
        PluggableViewModelFactory<PluginExec> factory = mock(PluggableViewModelFactory.class);
        ConfigTypeExtension exec = new TestTaskConfigTypeExtension<>(PluginExec.class, factory);
        PluginExec execInstance = new PluginExec();

        TaskViewModel stubbedViewModel = new TaskViewModel(execInstance, "my/view");
        when(factory.viewModelFor(execInstance, "new")).thenReturn(stubbedViewModel);


        ConfigurationExtension execTask = new ConfigurationExtension<>(
                new PluginNamespace(execCtx, new URL("file:///exec")), exec);
        when(pluginExtns.configTagImplementations()).thenReturn(Arrays.asList(execTask));

        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(pluginExtns);
        assertThat(registry.getViewModelFor(execInstance, "new"), is(stubbedViewModel));
    }

    @Test
    public void shouldNotThrowUpIfPluginHasNotRegisteredViewTemplates() throws Exception {
        BundleContext execCtx = PluginTestUtil.bundleCtxWithHeaders(DataStructureUtils.m(PluginNamespace.XSD_NAMESPACE_PREFIX, "exec", PluginNamespace.XSD_NAMESPACE_URI, "uri-exec"));
        ConfigTypeExtension exec = new TestTaskConfigTypeExtension<>(PluginExec.class, PluggableViewModelFactory.DOES_NOT_APPLY);
        PluginExec execInstance = new PluginExec();

        ConfigurationExtension execTask = new ConfigurationExtension<>(
                new PluginNamespace(execCtx, new URL("file:///exec")), exec);
        when(pluginExtns.configTagImplementations()).thenReturn(Arrays.asList(execTask));

        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(pluginExtns);

        try {
            registry.getViewModelFor(execInstance, "new");
            fail("Should not have a view model when the plugin factory does not exist");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format("This component does not support rendering '%s' for action 'new'", execInstance)));
        }
    }

    @Test
    public void registerAllConfigTagImplementationsProvidedByPlugins() throws MalformedURLException {
        BundleContext execCtx = PluginTestUtil.bundleCtxWithHeaders(DataStructureUtils.m(PluginNamespace.XSD_NAMESPACE_PREFIX, "exec", PluginNamespace.XSD_NAMESPACE_URI, "uri-exec"));
        PluggableViewModelFactory<PluginExec> factory = mock(PluggableViewModelFactory.class);
        ConfigTypeExtension exec = new TestTaskConfigTypeExtension<>(PluginExec.class, factory);

        ConfigurationExtension execTag = new ConfigurationExtension<>(
                new PluginNamespace(execCtx, new URL("file:///exec")), exec);

        BundleContext antCtx = PluginTestUtil.bundleCtxWithHeaders(DataStructureUtils.m(PluginNamespace.XSD_NAMESPACE_PREFIX, "ant", PluginNamespace.XSD_NAMESPACE_URI, "uri-ant"));

        ConfigTypeExtension ant = new TestTaskConfigTypeExtension<>(PluginAnt.class, mock(PluggableViewModelFactory.class));

        ConfigurationExtension antTag = new ConfigurationExtension<>(
                new PluginNamespace(antCtx, new URL("file:///ant")), ant);

        when(pluginExtns.configTagImplementations()).thenReturn(Arrays.asList(execTag, antTag));

        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(pluginExtns);

        assertThat(registry.xsds(), containsString("uri-exec file:/exec"));
        assertThat(registry.xsds(), containsString("uri-ant file:/ant"));

        List<Class<? extends Task>> implementationTypes = registry.implementersOf(Task.class);
        assertThat(implementationTypes.contains(PluginExec.class), is(true));
        assertThat(implementationTypes.contains(PluginAnt.class), is(true));

        Element mock = mock(Element.class);
        registry.registerNamespacesInto(mock);
        verify(mock).addNamespaceDeclaration(Namespace.getNamespace("exec", "uri-exec"));
        verify(mock).addNamespaceDeclaration(Namespace.getNamespace("ant", "uri-ant"));
    }

    class PluginAnt extends BuildTask {

        @Override
        public String getTaskType() {
            return "build";
        }

        @Override
        public String getTypeForDisplay() {
            return null;
        }

        @Override
        public String command() {
            return null;
        }

        @Override
        public String arguments() {
            return null;
        }
    }

    class PluginExec extends BuildTask {

        @Override
        public String getTaskType() {
            return "build";
        }

        @Override
        public String getTypeForDisplay() {
            return null;
        }

        @Override
        public String command() {
            return null;
        }

        @Override
        public String arguments() {
            return null;
        }
    }
}
