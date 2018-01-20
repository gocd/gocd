/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FelixGoPluginOSGiFrameworkTest {
    public static final String TEST_SYMBOLIC_NAME = "testplugin.descriptorValidator";
    private final GoPluginDescriptor descriptor = buildExpectedDescriptor();
    @Mock private BundleContext bundleContext;
    @Mock private Bundle bundle;
    @Mock private Framework framework;
    @Mock private PluginRegistry registry;
    @Mock private SystemEnvironment systemEnvironment;
    private FelixGoPluginOSGiFramework spy;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        FelixGoPluginOSGiFramework goPluginOSGiFramwork = new FelixGoPluginOSGiFramework(registry, systemEnvironment);

        spy = spy(goPluginOSGiFramwork);
        when(framework.getBundleContext()).thenReturn(bundleContext);
        when(registry.getPlugin(TEST_SYMBOLIC_NAME)).thenReturn(descriptor);
        doReturn(framework).when(spy).getFelixFramework(Matchers.<List<FrameworkFactory>>anyObject());
    }

    @After
    public void tearDown() throws Exception {
        spy.stop();
    }

    @Test
    public void shouldRegisterAnInstanceOfEachOfTheRequiredPluginServicesAfterOSGiFrameworkIsInitialized() {
        spy.start();

        verify(bundleContext).registerService(eq(PluginHealthService.class), any(DefaultPluginHealthService.class), isNull(Dictionary.class));
        verify(bundleContext).registerService(eq(LoggingService.class), any(DefaultPluginLoggingService.class), isNull(Dictionary.class));
    }

    @Test
    public void shouldRunAnActionOnAllRegisteredImplementationsOfAGivenInterface() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        registerServices(firstService, secondService);
        spy.start();

        spy.doOnAll(SomeInterface.class, new Action<SomeInterface>() {
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                obj.someMethod();
                assertThat(pluginDescriptor, is(descriptor));
            }
        });

        verify(firstService).someMethod();
        verify(secondService).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void shouldFailWithAnExceptionWhenAnExceptionHandlerIsNotProvided() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        SomeInterface thirdService = mock(SomeInterface.class);

        registerServices(firstService, secondService, thirdService);
        spy.start();

        RuntimeException exceptionToBeThrown = new RuntimeException("Ouch!");
        doThrow(exceptionToBeThrown).when(secondService).someMethod();

        try {
            spy.doOnAll(SomeInterface.class, new Action<SomeInterface>() {
                public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                    obj.someMethod();
                    assertThat(pluginDescriptor, is(descriptor));
                }
            });
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Ouch!"));
            assertThat(e.getCause().getMessage(), is("Ouch!"));
        }

        verify(firstService).someMethod();
        verify(secondService).someMethod();
        verifyZeroInteractions(thirdService);
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void shouldAllowHandlingExceptionsDuringRunningOfAnActionOnAllRegisteredImplementationsOfAGivenInterface() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        SomeInterface thirdService = mock(SomeInterface.class);

        registerServices(firstService, secondService, thirdService);
        spy.start();

        RuntimeException exceptionToBeThrown = new RuntimeException("Ouch!");

        ExceptionHandler<SomeInterface> exceptionHandler = mock(ExceptionHandler.class);
        doThrow(exceptionToBeThrown).when(secondService).someMethod();

        spy.doOnAllWithExceptionHandling(SomeInterface.class, new Action<SomeInterface>() {
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                obj.someMethod();
                assertThat(pluginDescriptor, is(descriptor));
            }
        }, exceptionHandler);

        InOrder inOrder = inOrder(firstService, secondService, thirdService, exceptionHandler);
        inOrder.verify(firstService).someMethod();
        inOrder.verify(secondService).someMethod();
        inOrder.verify(exceptionHandler).handleException(secondService, exceptionToBeThrown);
        inOrder.verify(thirdService).someMethod();

        verifyNoMoreInteractions(exceptionHandler, firstService, secondService, thirdService);
    }

    @Test
    public void shouldDoNothingWhenTryingToRunOnAllImplementationsIfPluginsAreNotEnabled() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);

        registerServices(firstService);

        spy.doOnAll(SomeInterface.class, new Action<SomeInterface>() {
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                obj.someMethod();
                assertThat(pluginDescriptor, is(descriptor));
            }
        });

        verifyZeroInteractions(firstService);

        ExceptionHandler exceptionHandler = mock(ExceptionHandler.class);
        spy.doOnAllWithExceptionHandling(SomeInterface.class, new Action<SomeInterface>() {
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                obj.someMethod();
            }
        }, exceptionHandler);

        verifyZeroInteractions(firstService, exceptionHandler);
    }

    @Test
    public void doOnShouldRunAnActionOnSpecifiedPluginImplementationsOfAGivenInterface() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        registerServices(firstService, secondService);
        spy.start();


        spy.doOn(SomeInterface.class, secondService.toString(), new ActionWithReturn<SomeInterface, Object>() {
            @Override
            public Object execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                assertThat(pluginDescriptor, is(descriptor));
                return obj.someMethodWithReturn();
            }
        });
        spy.doOn(SomeInterface.class, secondService.toString(), new Action<SomeInterface>() {
            @Override
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                assertThat(pluginDescriptor, is(descriptor));
                obj.someMethod();
            }
        });

        spy.doOnWithExceptionHandling(SomeInterface.class, secondService.toString(), new Action<SomeInterface>() {
                    @Override
                    public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                        assertThat(pluginDescriptor, is(descriptor));
                        obj.someMethod();
                    }
                }, new ExceptionHandler<SomeInterface>() {
                    @Override
                    public void handleException(SomeInterface obj, Throwable t) {
                    }
                }
        );

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService).someMethodWithReturn();
        verify(secondService, times(2)).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void doOnExceptionHandlingShouldRunAnActionOnSpecifiedPluginImplementationsOfAGivenInterfaceAndDelegateTheExceptionToTheHandler() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        registerServices(firstService, secondService);
        spy.start();

        final RuntimeException expectedException = new RuntimeException("Exception Thrown By Spy Method");
        spy.doOnWithExceptionHandling(SomeInterface.class, secondService.toString(), new Action<SomeInterface>() {
                    @Override
                    public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                        assertThat(pluginDescriptor, is(descriptor));
                        obj.someMethod();
                        throw expectedException;
                    }
                }, new ExceptionHandler<SomeInterface>() {
                    @Override
                    public void handleException(SomeInterface obj, Throwable t) {
                        assertThat(t, is(expectedException));
                    }
                }
        );

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void doOnShouldThrowAnExceptionWhenThereAreMultipleServicesWithSamePluginId_IdeallyThisShouldNotHappenInProductionSincePluginIdIsSymbolicName() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "same_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeInterface.class, symbolicName, new ActionWithReturn<SomeInterface, Object>() {
                @Override
                public Object execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                    assertThat(pluginDescriptor, is(descriptor));
                    return obj.someMethodWithReturn();
                }
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("More than one reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        try {
            spy.doOn(SomeInterface.class, symbolicName, new Action<SomeInterface>() {
                @Override
                public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                    assertThat(pluginDescriptor, is(descriptor));
                    obj.someMethod();
                }
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("More than one reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        try {
            spy.doOnWithExceptionHandling(SomeInterface.class, symbolicName, new Action<SomeInterface>() {
                        @Override
                        public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                            assertThat(pluginDescriptor, is(descriptor));
                            obj.someMethod();
                        }
                    }, new ExceptionHandler<SomeInterface>() {
                        @Override
                        public void handleException(SomeInterface obj, Throwable t) {

                        }
                    }
            );
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("More than one reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void doOnShouldThrowAnExceptionWhenThereAreNoServicesAreFoundForTheGivenFilterAndServiceReference() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "dummy_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeOtherInterface.class, symbolicName, new ActionWithReturn<SomeOtherInterface, Object>() {
                @Override
                public Object execute(SomeOtherInterface obj, GoPluginDescriptor pluginDescriptor) {
                    assertThat(pluginDescriptor, is(descriptor));
                    throw new RuntimeException("Should Not Be invoked");
                }
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("No reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeOtherInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        try {
            spy.doOn(SomeOtherInterface.class, symbolicName, new Action<SomeOtherInterface>() {
                @Override
                public void execute(SomeOtherInterface obj, GoPluginDescriptor pluginDescriptor) {
                    assertThat(pluginDescriptor, is(descriptor));
                    throw new RuntimeException("Should Not Be invoked");
                }
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("No reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeOtherInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        try {
            spy.doOnWithExceptionHandling(SomeOtherInterface.class, symbolicName, new Action<SomeOtherInterface>() {
                        @Override
                        public void execute(SomeOtherInterface obj, GoPluginDescriptor pluginDescriptor) {
                            assertThat(pluginDescriptor, is(descriptor));
                            throw new RuntimeException("Should Not Be invoked");
                        }
                    }, new ExceptionHandler<SomeOtherInterface>() {
                        @Override
                        public void handleException(SomeOtherInterface obj, Throwable t) {

                        }
                    }
            );
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("No reference found"), is(true));
            assertThat(ex.getMessage().contains(SomeOtherInterface.class.getCanonicalName()), is(true));
            assertThat(ex.getMessage().contains(symbolicName), is(true));
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void doOnAllShouldRunAnActionOnAllPluginExtensionsOfAGivenPluginJar() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "same_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, firstService, secondService);
        spy.start();
        spy.doOnAllForPlugin(SomeInterface.class, symbolicName, new Action<SomeInterface>() {
            @Override
            public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                assertThat(pluginDescriptor, is(descriptor));
                obj.someMethod();
            }
        });
        verify(secondService).someMethod();
        verify(firstService).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void doOnAllWithExceptionHandlingShouldRunAnActionOnAllPluginExtensionsOfAGivenPluginJar() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String symbolicName = "same_symbolic_name";
        registerServicesWithSameSymbolicName(symbolicName, firstService, secondService);
        spy.start();
        spy.doOnAllWithExceptionHandlingForPlugin(SomeInterface.class, symbolicName, new Action<SomeInterface>() {
                    @Override
                    public void execute(SomeInterface obj, GoPluginDescriptor pluginDescriptor) {
                        assertThat(pluginDescriptor, is(descriptor));
                        obj.someMethod();
                        throw new RuntimeException("Dummy Exception");
                    }
                }, new ExceptionHandler<SomeInterface>() {
                    @Override
                    public void handleException(SomeInterface obj, Throwable t) {

                    }
                }
        );
        verify(secondService).someMethod();
        verify(firstService).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    private void registerServicesWithSameSymbolicName(String symbolicName, SomeInterface... someInterfaces) throws InvalidSyntaxException {
        ArrayList<ServiceReference<SomeInterface>> references = new ArrayList<>();
        for (int i = 0; i < someInterfaces.length; ++i) {
            ServiceReference reference = mock(ServiceReference.class);
            Bundle bundle = mock(Bundle.class);
            when(reference.getBundle()).thenReturn(bundle);
            when(bundle.getSymbolicName()).thenReturn(TEST_SYMBOLIC_NAME);
            when(bundleContext.getService(reference)).thenReturn(someInterfaces[i]);
            references.add(reference);
        }
        String propertyFormat = String.format("(%s=%s)", Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(references);

    }

    @Test
    public void HasReferencesShouldReturnAppropriateValueIfSpecifiedPluginImplementationsOfAGivenInterfaceIsFoundOrNotFound() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        spy.start();

        boolean reference = spy.hasReferenceFor(SomeInterface.class, secondService.toString());

        assertThat(reference, is(false));
        registerServices(firstService, secondService);
        reference = spy.hasReferenceFor(SomeInterface.class, secondService.toString());
        assertThat(reference, is(true));

        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    public void shouldUnloadAPlugin() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        Bundle bundle = mock(Bundle.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);

        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, atLeastOnce()).stop();
        verify(bundle, atLeastOnce()).uninstall();
    }

    @Test
    public void shouldUnloadAnInvalidPlugin() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        Bundle bundle = mock(Bundle.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);
        when(pluginDescriptor.isInvalid()).thenReturn(true);

        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, atLeastOnce()).stop();
        verify(bundle, atLeastOnce()).uninstall();
    }

    @Test
    public void shouldNotUnloadBundleForAnUnloadedInvalidPlugin() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(null);

        spy.unloadPlugin(pluginDescriptor);
    }

    @Test
    public void shouldMarkThePluginAsInvalidIfAnyExceptionOccursAfterLoad() throws BundleException {
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
            assertTrue(goPluginDescriptor.getStatus().isInvalid());
        }
    }

    private void registerServices(SomeInterface... someInterfaces) throws InvalidSyntaxException {
        ArrayList<ServiceReference<SomeInterface>> references = new ArrayList<>();
        for (int i = 0; i < someInterfaces.length; ++i) {
            ServiceReference reference = mock(ServiceReference.class);
            when(reference.getBundle()).thenReturn(bundle);
            when(bundle.getSymbolicName()).thenReturn(TEST_SYMBOLIC_NAME);

            when(bundleContext.getService(reference)).thenReturn(someInterfaces[i]);
            setExpectationForFilterBasedServiceReferenceCall(someInterfaces[i], reference);
            references.add(reference);
        }
        when(bundleContext.getServiceReferences(SomeInterface.class, null)).thenReturn(references);

    }

    private void setExpectationForFilterBasedServiceReferenceCall(SomeInterface service, ServiceReference reference) throws InvalidSyntaxException {
        ArrayList<ServiceReference<SomeInterface>> references = new ArrayList<>();
        String propertyFormat = String.format("(%s=%s)", Constants.BUNDLE_SYMBOLICNAME, service.toString());
        references.add(reference);
        when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(references);
    }

    private GoPluginDescriptor buildExpectedDescriptor() {
        return new GoPluginDescriptor(TEST_SYMBOLIC_NAME, "1",
                new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                        new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows")), null, null, true
        );
    }

    private interface SomeInterface {
        void someMethod();

        Object someMethodWithReturn();
    }

    private interface SomeOtherInterface {
    }
}
