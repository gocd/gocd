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

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginHealthService;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.felix.framework.util.FelixConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class FelixGoPluginOSGiFrameworkTest {
    private static final String TEST_SYMBOLIC_NAME = "testplugin.descriptorValidator";

    @Mock
    private BundleContext bundleContext;
    @Mock
    private Bundle bundle;
    @Mock
    private Framework framework;
    @Mock
    private PluginRegistry registry;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator;
    private FelixGoPluginOSGiFramework spy;

    @BeforeEach
    void setUp() {
        initMocks(this);
        FelixGoPluginOSGiFramework goPluginOSGiFramework = new FelixGoPluginOSGiFramework(registry, systemEnvironment, pluginExtensionsAndVersionValidator) {
            @Override
            protected HashMap<String, String> generateOSGiFrameworkConfig() {
                HashMap<String, String> config = super.generateOSGiFrameworkConfig();
                config.put(FelixConstants.RESOLVER_PARALLELISM, "1");
                return config;
            }
        };

        spy = spy(goPluginOSGiFramework);
        when(framework.getBundleContext()).thenReturn(bundleContext);
        when(registry.getPlugin(TEST_SYMBOLIC_NAME)).thenReturn(buildExpectedDescriptor(TEST_SYMBOLIC_NAME));
        doReturn(framework).when(spy).getFelixFramework(any());
    }

    @AfterEach
    void tearDown() {
        spy.stop();
    }

    @Test
    void shouldRegisterAnInstanceOfEachOfTheRequiredPluginServicesAfterOSGiFrameworkIsInitialized() {
        spy.start();

        verify(bundleContext).registerService(eq(PluginHealthService.class), any(DefaultPluginHealthService.class), isNull(Dictionary.class));
        verify(bundleContext).registerService(eq(LoggingService.class), any(DefaultPluginLoggingService.class), isNull(Dictionary.class));
    }

    @Test
    void doOnShouldRunAnActionOnSpecifiedPluginImplementationsOfAGivenInterface() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        registerService(firstService, "plugin-one", "extension-one");
        registerService(secondService, "plugin-two", "extension-two");
        spy.start();

        spy.doOn(SomeInterface.class, "plugin-two", "extension-two", (obj, pluginDescriptor) -> {
            assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor("plugin-two"));
            return obj.someMethodWithReturn();
        });

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService).someMethodWithReturn();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void doOnShouldThrowAnExceptionWhenThereAreMultipleServicesWithSamePluginIdAndSameExtensionType_IdeallyThisShouldNotHappenInProductionSincePluginIdIsSymbolicName() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "same_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, "test-extension", firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeInterface.class, symbolicName, "test-extension", (obj, pluginDescriptor) -> {
                assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor(symbolicName));
                return obj.someMethodWithReturn();
            });
            fail("Should throw plugin framework exception");
        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("More than one reference found")).isTrue();
            assertThat(ex.getMessage().contains(SomeInterface.class.getCanonicalName())).isTrue();
            assertThat(ex.getMessage().contains(symbolicName)).isTrue();
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void doOnShouldThrowAnExceptionWhenNoServicesAreFoundForTheGivenFilterAndServiceReference() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "dummy_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, "test-extension", firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeOtherInterface.class, symbolicName, "test-extension", (obj, pluginDescriptor) -> {
                assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor(symbolicName));
                throw new RuntimeException("Should Not Be invoked");
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("No reference found")).isTrue();
            assertThat(ex.getMessage().contains(SomeOtherInterface.class.getCanonicalName())).isTrue();
            assertThat(ex.getMessage().contains(symbolicName)).isTrue();
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void hasReferencesShouldReturnAppropriateValueIfSpecifiedPluginImplementationsOfAGivenInterfaceIsFoundOrNotFound() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        spy.start();

        boolean reference = spy.hasReferenceFor(SomeInterface.class, secondService.toString(), "extension-two");

        assertThat(reference).isFalse();

        registerService(firstService, "plugin-one", "extension-one");
        registerService(secondService, "plugin-two", "extension-two");

        reference = spy.hasReferenceFor(SomeInterface.class, "plugin-two", "extension-two");
        assertThat(reference).isTrue();

        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void shouldUnloadAPlugin() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);

        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, atLeastOnce()).stop();
        verify(bundle, atLeastOnce()).uninstall();
    }

    @Test
    void shouldNotFailToUnloadAPluginWhoseBundleIsNull() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(null);

        try {
            spy.unloadPlugin(pluginDescriptor);
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    @Test
    void shouldNotFailToUnloadAPluginWhenAPluginUnloadListenerFails() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);

        PluginChangeListener listenerWhichThrowsWhenUnloading = mock(PluginChangeListener.class);
        doThrow(new RuntimeException("Fail!")).when(listenerWhichThrowsWhenUnloading).pluginUnLoaded(pluginDescriptor);

        spy.addPluginChangeListener(listenerWhichThrowsWhenUnloading);
        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, times(1)).stop();
        verify(bundle, times(1)).uninstall();
    }

    @Test
    void shouldRunOtherUnloadListenersEvenIfOneFails() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);

        PluginChangeListener listenerWhichWorks1 = mock(PluginChangeListener.class, "Listener Which Works: 1");
        PluginChangeListener listenerWhichWorks2 = mock(PluginChangeListener.class, "Listener Which Works: 2");
        PluginChangeListener listenerWhichThrowsWhenUnloading = mock(PluginChangeListener.class, "Listener Which Throws");
        doThrow(new RuntimeException("Fail!")).when(listenerWhichThrowsWhenUnloading).pluginUnLoaded(pluginDescriptor);

        spy.addPluginChangeListener(listenerWhichWorks1);
        spy.addPluginChangeListener(listenerWhichThrowsWhenUnloading);
        spy.addPluginChangeListener(listenerWhichWorks2);

        spy.unloadPlugin(pluginDescriptor);

        verify(listenerWhichWorks1, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichThrowsWhenUnloading, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichWorks2, times(1)).pluginUnLoaded(pluginDescriptor);

        verify(bundle, times(1)).stop();
        verify(bundle, times(1)).uninstall();
    }

    @Test
    void shouldMarkThePluginAsInvalidIfAnyExceptionOccursAfterLoad() throws BundleException {
        final Bundle bundle = mock(Bundle.class);
        spy.addPluginChangeListener(new PluginChangeListener() {
            @Override
            public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
                throw new RuntimeException("some error");
            }

            @Override
            public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
            }
        });
        when(bundleContext.installBundle(any(String.class))).thenReturn(bundle);
        final GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor(TEST_SYMBOLIC_NAME, "1.0", null, "location", new File(""), false);

        spy.start();
        try {
            spy.loadPlugin(goPluginDescriptor);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(goPluginDescriptor.getStatus().isInvalid()).isTrue();
        }
    }

    @Test
    void shouldNotifyAllPluginChangeListenerOncePluginIsLoaded() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        final PluginExtensionsAndVersionValidator.ValidationResult result = mock(PluginExtensionsAndVersionValidator.ValidationResult.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);
        when(pluginDescriptor.bundleLocation()).thenReturn(new File("foo"));
        when(bundleContext.installBundle(any(String.class))).thenReturn(bundle);
        when(pluginExtensionsAndVersionValidator.validate(pluginDescriptor)).thenReturn(result);
        when(result.hasError()).thenReturn(false);

        PluginChangeListener listener1 = mock(PluginChangeListener.class);
        PluginChangeListener listener2 = mock(PluginChangeListener.class);
        PluginChangeListener listener3 = mock(PluginChangeListener.class);

        spy.addPluginChangeListener(listener1);
        spy.addPluginChangeListener(listener2);
        spy.addPluginChangeListener(listener3);
        spy.start();

        spy.loadPlugin(pluginDescriptor);

        verify(listener1, times(1)).pluginLoaded(pluginDescriptor);
        verify(listener3, times(1)).pluginLoaded(pluginDescriptor);
        verify(listener2, times(1)).pluginLoaded(pluginDescriptor);

        verify(bundle, times(1)).start();
    }

    @Test
    void shouldMarkPluginDescriptorInvalidAndNotNotifyPluginChangeListenersWhenExtensionVersionsRequiredByThePluginIsNotSupportedByGoCD() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        final PluginExtensionsAndVersionValidator.ValidationResult result = mock(PluginExtensionsAndVersionValidator.ValidationResult.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);
        when(pluginDescriptor.bundleLocation()).thenReturn(new File("foo"));
        when(bundleContext.installBundle(any(String.class))).thenReturn(bundle);
        when(pluginExtensionsAndVersionValidator.validate(pluginDescriptor)).thenReturn(result);
        when(result.hasError()).thenReturn(true);

        PluginChangeListener listener1 = mock(PluginChangeListener.class);
        PluginChangeListener listener2 = mock(PluginChangeListener.class);
        PluginChangeListener listener3 = mock(PluginChangeListener.class);

        spy.addPluginChangeListener(listener1);
        spy.addPluginChangeListener(listener2);
        spy.addPluginChangeListener(listener3);

        spy.start();
        spy.loadPlugin(pluginDescriptor);

        verifyZeroInteractions(listener1);
        verifyZeroInteractions(listener3);
        verifyZeroInteractions(listener2);

        verify(bundle, times(1)).start();
        verify(pluginDescriptor).markAsInvalid(anyList(), eq(null));
    }

    private void registerServicesWithSameSymbolicName(String symbolicName, String extensionType, SomeInterface... someInterfaces) throws InvalidSyntaxException {
        ArrayList<ServiceReference<SomeInterface>> references = new ArrayList<>();

        for (SomeInterface someInterface : someInterfaces) {
            ServiceReference<SomeInterface> reference = mock(ServiceReference.class);
            Bundle bundle = mock(Bundle.class);
            when(reference.getBundle()).thenReturn(bundle);
            when(bundle.getSymbolicName()).thenReturn(symbolicName);
            when(bundleContext.getService(reference)).thenReturn(someInterface);
            references.add(reference);
        }

        String propertyFormat = String.format("(&(%s=%s)(%s=%s))", Constants.BUNDLE_SYMBOLICNAME, symbolicName, Constants.BUNDLE_CATEGORY, extensionType);
        when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(references);
        when(registry.getPlugin(symbolicName)).thenReturn(buildExpectedDescriptor(symbolicName));
    }

    private void registerService(SomeInterface someInterface, String pluginID, String extension) throws InvalidSyntaxException {
        ServiceReference<SomeInterface> reference = mock(ServiceReference.class);

        when(reference.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn(pluginID);
        when(bundleContext.getService(reference)).thenReturn(someInterface);
        when(registry.getPlugin(pluginID)).thenReturn(buildExpectedDescriptor(pluginID));

        String propertyFormat = String.format("(&(%s=%s)(%s=%s))", Constants.BUNDLE_SYMBOLICNAME, pluginID, Constants.BUNDLE_CATEGORY, extension);
        when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(Collections.singletonList(reference));

        when(bundleContext.getServiceReferences(SomeInterface.class, null)).thenReturn(Collections.singletonList(reference));
    }

    private GoPluginDescriptor buildExpectedDescriptor(String pluginID) {
        return new GoPluginDescriptor(pluginID, "1",
                new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "17.12", "Validates its own plugin descriptor",
                        new GoPluginDescriptor.Vendor("ThoughtWorks GoCD Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows")), null, null, true
        );
    }

    private interface SomeInterface {
        void someMethod();

        Object someMethodWithReturn();
    }

    private interface SomeOtherInterface {
    }
}
