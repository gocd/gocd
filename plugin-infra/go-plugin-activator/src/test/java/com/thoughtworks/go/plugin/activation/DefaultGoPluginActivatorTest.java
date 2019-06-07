/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.activation;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginRegistryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DefaultGoPluginActivatorTest {
    private static final String CONSTRUCTOR_FAIL_MSG = "Ouch! Failed construction";
    private static final String PLUGIN_ID = "plugin-id";
    private static final String SYMBOLIC_NAME = "plugin-id";
    private static final String NO_EXT_ERR_MSG = "No extensions found in this plugin.Please check for @Extension annotations";

    private DefaultGoPluginActivator activator;
    @Captor private ArgumentCaptor<List<String>> errorMessageCaptor;
    @Mock private BundleContext context;
    @Mock private Bundle bundle;
    @Mock private ServiceReference<PluginRegistryService> pluginRegistryServiceReference;
    @Mock private PluginRegistryService pluginRegistryService;
    @Mock private ServiceReference<LoggingService> loggingServiceReference;
    @Mock private LoggingService loggingService;
    private Enumeration<URL> emptyListOfClassesInBundle = new Hashtable<URL, String>().keys();

    @Before
    public void setUp() {
        initMocks(this);

        when(context.getServiceReference(PluginRegistryService.class)).thenReturn(pluginRegistryServiceReference);
        when(context.getServiceReference(LoggingService.class)).thenReturn(loggingServiceReference);
        when(context.getService(pluginRegistryServiceReference)).thenReturn(pluginRegistryService);
        when(context.getService(loggingServiceReference)).thenReturn(loggingService);

        when(context.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn(PLUGIN_ID);
        when(bundle.findEntries("/", "*.class", true)).thenReturn(emptyListOfClassesInBundle);
        when(pluginRegistryService.getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME)).thenReturn(PLUGIN_ID);

        activator = new DefaultGoPluginActivator();
    }

    @After
    public void tearDown() throws Exception {
        Logger.initialize(null);
    }

    @Test
    public void shouldReportAClassLoadErrorToThePluginHealthService() throws Exception {
        setupClassesInBundle("SomeClass.class");
        when(bundle.loadClass(anyString())).thenThrow(new ClassNotFoundException("Ouch! Failed"));

        activator.start(context);

        verifyErrorsReported("Class [SomeClass] could not be loaded. Message: [Ouch! Failed].", NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldReportMultipleClassLoadErrorsToThePluginHealthService() throws Exception {
        setupClassesInBundle("SomeClass.class", "SomeOtherClass.class");
        when(bundle.loadClass(anyString())).thenThrow(new ClassNotFoundException("Ouch! Failed"));

        activator.start(context);

        verifyErrorsReported("Class [SomeClass] could not be loaded. Message: [Ouch! Failed].", "Class [SomeOtherClass] could not be loaded. Message: [Ouch! Failed]."
                , NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItIsNotPublic() throws Exception {
        setupClassesInBundle("NonPublicGoExtensionClass.class");
        when(bundle.loadClass(contains("NonPublicGoExtensionClass"))).thenReturn((Class) NonPublicGoExtensionClass.class);

        activator.start(context);

        verifyErrorsReported("Class [NonPublicGoExtensionClass] is annotated with @Extension but is not public.", NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldNotReportAClassWhichIsNotAnnotatedAsAnExtensionEvenIfItIsNotPublic() throws Exception {
        setupClassesInBundle("NonPublicClassWhichIsNotAGoExtension.class");
        when(bundle.loadClass(contains("NonPublicClassWhichIsNotAGoExtension"))).thenReturn((Class) NonPublicClassWhichIsNotAGoExtension.class);

        activator.start(context);

        verifyErrorsReported(NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItIsAbstract() throws Exception {
        setupClassesInBundle("PublicAbstractGoExtensionClass.class");
        when(bundle.loadClass(contains("PublicAbstractGoExtensionClass"))).thenReturn((Class) PublicAbstractGoExtensionClass.class);

        activator.start(context);

        verifyErrorsReported("Class [PublicAbstractGoExtensionClass] is annotated with @Extension but is abstract.", NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItIsNotInstantiable() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor.class");
        when(bundle.loadClass(contains("PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor"))).thenReturn((Class) PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor.class);

        activator.start(context);

        verifyErrorsReported("Class [PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor] is annotated with @Extension but cannot be constructed. "
                + "Make sure it and all of its parent classes have a default constructor.", NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItFailsDuringConstruction() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor.class");
        when(bundle.loadClass(contains("PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor"))).thenReturn((Class) PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor.class);

        activator.start(context);

        verifyErrorsReported(
                format("Class [PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor] is annotated with @Extension but cannot be constructed. Reason: java.lang.RuntimeException: %s.",
                        CONSTRUCTOR_FAIL_MSG), NO_EXT_ERR_MSG);
    }

    @Test
    public void shouldSetupTheLoggerWithTheLoggingServiceAndPluginId() throws Exception {
        setupClassesInBundle();

        activator.start(context);

        Logger logger = Logger.getLoggerFor(DefaultGoPluginActivatorTest.class);
        logger.info("INFO");

        verify(loggingService).info(PLUGIN_ID, DefaultGoPluginActivatorTest.class.getName(), "INFO");
    }

    @Test
    public void loggerShouldBeAvailableToBeUsedInStaticBlocksAndConstructorAndLoadUnloadMethodsOfPluginExtensionClasses() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichLogsInAStaticBlock.class");
        when(bundle.loadClass(contains("PublicGoExtensionClassWhichLogsInAStaticBlock"))).thenReturn((Class) PublicGoExtensionClassWhichLogsInAStaticBlock.class);

        activator.start(context);
        activator.stop(context);

        verify(loggingService).info(PLUGIN_ID, PublicGoExtensionClassWhichLogsInAStaticBlock.class.getName(), "HELLO from static block in PublicGoExtensionClassWhichLogsInAStaticBlock");
        verify(loggingService).info(PLUGIN_ID, PublicGoExtensionClassWhichLogsInAStaticBlock.class.getName(), "HELLO from constructor in PublicGoExtensionClassWhichLogsInAStaticBlock");
        verify(loggingService).info(PLUGIN_ID, PublicGoExtensionClassWhichLogsInAStaticBlock.class.getName(), "HELLO from load in PublicGoExtensionClassWhichLogsInAStaticBlock");
        verify(loggingService).info(PLUGIN_ID, PublicGoExtensionClassWhichLogsInAStaticBlock.class.getName(), "HELLO from unload in PublicGoExtensionClassWhichLogsInAStaticBlock");
    }

    @Test
    public void shouldInvokeMethodWithLoadUnloadAnnotationAtPluginStart() throws Exception {
        setupClassesInBundle("GoExtensionWithLoadUnloadAnnotation.class");
        when(bundle.loadClass(contains("GoExtensionWithLoadUnloadAnnotation"))).thenReturn((Class) GoExtensionWithLoadUnloadAnnotation.class);

        activator.start(context);

        assertThat(GoExtensionWithLoadUnloadAnnotation.loadInvoked, is(1));

        activator.stop(context);

        assertThat(GoExtensionWithLoadUnloadAnnotation.unLoadInvoked, is(1));
    }

    @Test
    public void shouldNotInvokeMethodWithLoadUnloadAnnotationAtPluginStartIfTheClassIsNotAnExtension() throws Exception {
        assertDidNotInvokeLoadUnload(NonExtensionWithLoadUnloadAnnotation.class);
    }

    @Test
    public void shouldNotInvokeStaticMethodWithLoadAnnotationAtPluginStart() throws Exception {
        assertDidNotInvokeLoadUnload(GoExtensionWithStaticLoadAnnotationMethod.class);
    }

    @Test
    public void shouldNotInvokeNonPublicMethodWithLoadAnnotationAtPluginStart() throws Exception {
        assertDidNotInvokeLoadUnload(GoExtensionWithNonPublicLoadUnloadAnnotation.class);
    }

    @Test
    public void shouldNotInvokePublicMethodWithLoadAnnotationHavingArgumentsAtPluginStart() throws Exception {
        assertDidNotInvokeLoadUnload(GoExtensionWithPublicLoadUnloadAnnotationWithArguments.class);
    }

    @Test
    public void shouldNotInvokeInheritedPublicMethodWithLoadAnnotationAtPluginStart() throws Exception {
        assertDidNotInvokeLoadUnload(GoExtensionWithInheritedPublicLoadUnloadAnnotationMethod.class);
    }

    private void assertDidNotInvokeLoadUnload(Class<?> testExtensionClass) throws Exception {
        assertLoadUnloadInvocationCount(testExtensionClass, 0);
    }

    private void assertLoadUnloadInvocationCount(Class<?> testExtensionClass, int invocationCount) throws Exception {
        String simpleNameOfTestExtensionClass = testExtensionClass.getSimpleName();
        setupClassesInBundle(simpleNameOfTestExtensionClass + ".class");
        when(bundle.loadClass(contains(simpleNameOfTestExtensionClass))).thenReturn((Class) testExtensionClass);

        activator.start(context);

        assertThat(testExtensionClass.getField("loadInvoked").getInt(null), is(invocationCount));

        activator.stop(context);

        assertThat(testExtensionClass.getField("unLoadInvoked").getInt(null), is(invocationCount));
    }

    @Test
    public void shouldGenerateExceptionWhenThereAreMoreThanOneLoadAnnotationsAtPluginStart() throws Exception {
        String expectedErrorMessageWithMethodsWithIncreasingOrder = "Class [GoExtensionWithMultipleLoadUnloadAnnotation] is annotated with @Extension will not be registered. "
                + "Reason: java.lang.RuntimeException: More than one method with @Load annotation not allowed. "
                + "Methods Found: [public void com.thoughtworks.go.plugin.activation.GoExtensionWithMultipleLoadUnloadAnnotation.setupData1(com.thoughtworks.go.plugin.api.info.PluginContext), "
                + "public void com.thoughtworks.go.plugin.activation.GoExtensionWithMultipleLoadUnloadAnnotation.setupData2(com.thoughtworks.go.plugin.api.info.PluginContext)].";

        String expectedErrorMessageWithMethodsWithDecreasingOrder = "Class [GoExtensionWithMultipleLoadUnloadAnnotation] is annotated with @Extension will not be registered. "
                + "Reason: java.lang.RuntimeException: More than one method with @Load annotation not allowed. "
                + "Methods Found: [public void com.thoughtworks.go.plugin.activation.GoExtensionWithMultipleLoadUnloadAnnotation.setupData2(com.thoughtworks.go.plugin.api.info.PluginContext), "
                + "public void com.thoughtworks.go.plugin.activation.GoExtensionWithMultipleLoadUnloadAnnotation.setupData1(com.thoughtworks.go.plugin.api.info.PluginContext)].";

        setupClassesInBundle("GoExtensionWithMultipleLoadUnloadAnnotation.class");
        when(bundle.loadClass(contains("GoExtensionWithMultipleLoadUnloadAnnotation"))).thenReturn((Class) GoExtensionWithMultipleLoadUnloadAnnotation.class);

        activator.start(context);
        assertThat(activator.hasErrors(), is(true));
        verifyThatOneOfTheErrorMessagesIsPresent(expectedErrorMessageWithMethodsWithIncreasingOrder, expectedErrorMessageWithMethodsWithDecreasingOrder);

        activator.stop(context);
        verifyThatOneOfTheErrorMessagesIsPresent(expectedErrorMessageWithMethodsWithIncreasingOrder, expectedErrorMessageWithMethodsWithDecreasingOrder);
    }

    @Test
    public void shouldHandleExceptionGeneratedByLoadMethodAtPluginStart() throws Exception {
        setupClassesInBundle("GoExtensionWithLoadAnnotationMethodThrowingException.class");
        when(bundle.loadClass(contains("GoExtensionWithLoadAnnotationMethodThrowingException"))).thenReturn((Class) GoExtensionWithLoadAnnotationMethodThrowingException.class);
        activator.start(context);
        assertThat(activator.hasErrors(), is(true));
        verifyErrorsReported("Class [GoExtensionWithLoadAnnotationMethodThrowingException] is annotated with @Extension but cannot be registered. "
                + "Reason: java.io.IOException: Load Dummy Checked Exception.");
    }

    @Test
    public void shouldHandleExceptionGeneratedByUnLoadMethodAtPluginStop() throws Exception {
        setupClassesInBundle("GoExtensionWithUnloadAnnotationMethodThrowingException.class");
        when(bundle.loadClass(contains("GoExtensionWithUnloadAnnotationMethodThrowingException"))).thenReturn((Class) GoExtensionWithUnloadAnnotationMethodThrowingException.class);
        activator.start(context);
        assertThat(activator.hasErrors(), is(false));
        activator.stop(context);
        assertThat(activator.hasErrors(), is(true));
        verifyErrorsReported("Invocation of unload method [public int com.thoughtworks.go.plugin.activation.GoExtensionWithUnloadAnnotationMethodThrowingException"
                + ".throwExceptionAgain(com.thoughtworks.go.plugin.api.info.PluginContext) "
                + "throws java.io.IOException]. "
                + "Reason: java.io.IOException: Unload Dummy Checked Exception.");
    }

    @Test
    public void shouldRegisterServiceWithBothPluginIDAndExtensionTypeAsProperties() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);

        Hashtable<String, String> expectedPropertiesUponRegistration = new Hashtable<>();
        expectedPropertiesUponRegistration.put(Constants.BUNDLE_SYMBOLICNAME, PLUGIN_ID);
        expectedPropertiesUponRegistration.put(Constants.BUNDLE_CATEGORY, "test-extension");

        activator.start(context);

        assertThat(activator.hasErrors(), is(false));
        verify(context).registerService(eq(GoPlugin.class), any(GoPlugin.class), eq(expectedPropertiesUponRegistration));
    }

    @Test
    public void shouldFailToRegisterServiceWhenExtensionTypeCannotBeSuccessfullyRetrieved() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier.class);

        activator.start(context);

        assertThat(activator.hasErrors(), is(true));
        verifyErrorReportedContains("Unable to find extension type from plugin identifier in class com.thoughtworks.go.plugin.activation.PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier");
        verify(context, times(0)).registerService(eq(GoPlugin.class), any(GoPlugin.class), any());
    }

    private void verifyThatOneOfTheErrorMessagesIsPresent(String expectedErrorMessage1, String expectedErrorMessage2) {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(pluginRegistryService).getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME);
        verify(pluginRegistryService).reportErrorAndInvalidate(eq(PLUGIN_ID), captor.capture());
        verifyNoMoreInteractions(pluginRegistryService);

        String actualErrorMessage = (String) captor.getValue().get(0);
        assertTrue(expectedErrorMessage1.equals(actualErrorMessage) || expectedErrorMessage2.equals(actualErrorMessage));
    }

    private void setupClassesInBundle(String... classes) throws MalformedURLException, ClassNotFoundException {
        Hashtable<URL, String> classFileEntries = new Hashtable<>();
        for (String aClass : classes) {
            classFileEntries.put(new URL("file:///" + aClass), "");
        }
        when(bundle.findEntries("/", "*.class", true)).thenReturn(classFileEntries.keys());
    }

    private void verifyErrorsReported(String... errors) {
        verify(pluginRegistryService).getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME);
        verify(pluginRegistryService).reportErrorAndInvalidate(PLUGIN_ID, asList(errors));
        verifyNoMoreInteractions(pluginRegistryService);
    }

    private void verifyErrorReportedContains(String expectedPartOfErrorMessage) {
        verify(pluginRegistryService).reportErrorAndInvalidate(eq(PLUGIN_ID), errorMessageCaptor.capture());
        List<String> errors = errorMessageCaptor.getValue();
        for (String errorMessage : errors) {
            if (errorMessage.contains(expectedPartOfErrorMessage)) {
                return;
            }
        }

        fail("Could not find error message with " + expectedPartOfErrorMessage + " in " + errors);
    }

    @Extension
    public abstract class PublicAbstractGoExtensionClass {
    }

    @Extension
    public class PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor implements GoPlugin {
        public PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor(int x) {
        }

        @Override
        public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        }

        @Override
        public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) throws UnhandledRequestTypeException {
            return null;
        }

        @Override
        public GoPluginIdentifier pluginIdentifier() {
            return null;
        }
    }

    @Extension
    public class PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor implements GoPlugin {
        public PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor() {
            throw new RuntimeException(CONSTRUCTOR_FAIL_MSG);
        }

        @Override
        public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        }

        @Override
        public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) throws UnhandledRequestTypeException {
            return null;
        }

        @Override
        public GoPluginIdentifier pluginIdentifier() {
            return null;
        }
    }

}

@Extension
class NonPublicGoExtensionClass {
}

class NonPublicClassWhichIsNotAGoExtension {
}
