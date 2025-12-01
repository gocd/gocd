/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginRegistryService;
import com.thoughtworks.go.util.UrlUtil;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.net.URL;
import java.util.*;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
public class DefaultGoPluginActivatorTest {
    private static final String CONSTRUCTOR_FAIL_MSG = "Ouch! Failed construction";
    private static final String PLUGIN_ID = "plugin-id";
    private static final String SYMBOLIC_NAME = "plugin-id";
    private static final String NO_EXT_ERR_MSG = "No extensions found in this plugin. Please check for @Extension annotations";

    private @Captor ArgumentCaptor<List<String>> errorMessageCaptor;
    private @Captor ArgumentCaptor<Dictionary<String, String>> propertiesCaptor;
    private @Mock BundleContext context;
    private @Mock(strictness = Mock.Strictness.LENIENT) Bundle bundle;
    private @Mock ServiceReference<PluginRegistryService> pluginRegistryServiceReference;
    private @Mock PluginRegistryService pluginRegistryService;
    private @Mock ServiceReference<LoggingService> loggingServiceReference;
    private @Mock LoggingService loggingService;

    private DefaultGoPluginActivator activator;

    @BeforeEach
    public void setUp() {
        when(context.getServiceReference(PluginRegistryService.class)).thenReturn(pluginRegistryServiceReference);
        when(context.getServiceReference(LoggingService.class)).thenReturn(loggingServiceReference);
        when(context.getService(pluginRegistryServiceReference)).thenReturn(pluginRegistryService);
        when(context.getService(loggingServiceReference)).thenReturn(loggingService);

        when(context.getBundle()).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn(SYMBOLIC_NAME);
        when(bundle.findEntries("/", "*.class", true)).thenReturn(Collections.emptyEnumeration());
        when(pluginRegistryService.getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME)).thenReturn(PLUGIN_ID);

        activator = new DefaultGoPluginActivator();
    }

    @AfterEach
    public void tearDown() {
        Logger.initialize(null);
    }

    @Test
    public void shouldReportAClassLoadErrorToThePluginHealthService() throws Exception {
        setupClassesInBundle("SomeClass.class");
        when(bundle.loadClass(anyString())).thenThrow(new ClassNotFoundException("Ouch! Failed"));

        activator.start(context);

        verifyErrorsReported("Class [SomeClass] could not be loaded. Message: [Ouch! Failed].");
    }

    @Test
    public void shouldReportMultipleClassLoadErrorsToThePluginHealthService() throws Exception {
        setupClassesInBundle("SomeClass.class", "SomeOtherClass.class");
        when(bundle.loadClass(anyString())).thenThrow(new ClassNotFoundException("Ouch! Failed"));

        activator.start(context);

        verifyErrorsReported("Class [SomeClass] could not be loaded. Message: [Ouch! Failed].", "Class [SomeOtherClass] could not be loaded. Message: [Ouch! Failed].");
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItIsNotPublic() throws Exception {
        setupClassesInBundle("NonPublicGoExtensionClass.class");
        when(bundle.loadClass(contains("NonPublicGoExtensionClass"))).thenReturn((Class) NonPublicGoExtensionClass.class);

        activator.start(context);

        verifyErrorsReported("Class [NonPublicGoExtensionClass] is annotated with @Extension but is not public.");
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
        when(pluginRegistryService.extensionClassesInBundle(any())).thenReturn(new ArrayList<>());
        when(bundle.loadClass(contains("PublicAbstractGoExtensionClass"))).thenReturn((Class) PublicAbstractGoExtensionClass.class);

        activator.start(context);

        verifyErrorsReported("Class [PublicAbstractGoExtensionClass] is annotated with @Extension but is abstract.");
    }

    @Test
    public void shouldReportAClassWhichIsAnnotatedAsAnExtensionIfItIsNotInstantiable() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor.class");
        when(bundle.loadClass(contains("PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor"))).thenReturn((Class) PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor.class);

        activator.start(context);

        verifyErrorsReported("Class [PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor] is annotated with @Extension but cannot be constructed. "
            + "Make sure it and all of its parent classes have a default constructor.");
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
    public void shouldSetupTheLoggerWithTheLoggingServiceAndPluginId() {
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

        assertThat(GoExtensionWithLoadUnloadAnnotation.loadInvoked).isEqualTo(1);

        activator.stop(context);

        assertThat(GoExtensionWithLoadUnloadAnnotation.unLoadInvoked).isEqualTo(1);
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

    @SuppressWarnings("SameParameterValue")
    private void assertLoadUnloadInvocationCount(Class<?> testExtensionClass, int invocationCount) throws Exception {
        String simpleNameOfTestExtensionClass = testExtensionClass.getSimpleName();
        setupClassesInBundle(simpleNameOfTestExtensionClass + ".class");
        when(bundle.loadClass(contains(simpleNameOfTestExtensionClass))).thenReturn((Class) testExtensionClass);

        activator.start(context);

        assertThat(testExtensionClass.getField("loadInvoked").getInt(null)).isEqualTo(invocationCount);

        activator.stop(context);

        assertThat(testExtensionClass.getField("unLoadInvoked").getInt(null)).isEqualTo(invocationCount);
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
        verify(pluginRegistryService).pluginIDFor(eq(SYMBOLIC_NAME), anyString());
        verifyThatOneOfTheErrorMessagesIsPresent(expectedErrorMessageWithMethodsWithIncreasingOrder, expectedErrorMessageWithMethodsWithDecreasingOrder);

        activator.stop(context);
        verifyThatOneOfTheErrorMessagesIsPresent(expectedErrorMessageWithMethodsWithIncreasingOrder, expectedErrorMessageWithMethodsWithDecreasingOrder);
    }

    @Test
    public void shouldHandleExceptionGeneratedByLoadMethodAtPluginStart() throws Exception {
        setupClassesInBundle("GoExtensionWithLoadAnnotationMethodThrowingException.class");
        when(bundle.loadClass(contains("GoExtensionWithLoadAnnotationMethodThrowingException"))).thenReturn((Class) GoExtensionWithLoadAnnotationMethodThrowingException.class);
        when(pluginRegistryService.pluginIDFor(eq(SYMBOLIC_NAME), anyString())).thenReturn(PLUGIN_ID);

        activator.start(context);

        assertThat(activator.hasErrors()).isEqualTo(true);

        verify(pluginRegistryService).pluginIDFor(eq(SYMBOLIC_NAME), anyString());
        verifyErrorsReported("Class [GoExtensionWithLoadAnnotationMethodThrowingException] is annotated with @Extension but cannot be registered. "
                + "Reason: java.io.IOException: Load Dummy Checked Exception.");
    }

    @Test
    public void shouldHandleExceptionGeneratedByUnLoadMethodAtPluginStop() throws Exception {
        setupClassesInBundle("GoExtensionWithUnloadAnnotationMethodThrowingException.class");
        when(pluginRegistryService.pluginIDFor(eq(SYMBOLIC_NAME), anyString())).thenReturn(PLUGIN_ID);
        when(bundle.loadClass(contains("GoExtensionWithUnloadAnnotationMethodThrowingException"))).thenReturn((Class) GoExtensionWithUnloadAnnotationMethodThrowingException.class);

        activator.start(context);

        assertActivatorHasNoErrors();
        activator.stop(context);

        assertThat(activator.hasErrors()).isEqualTo(true);
        verify(pluginRegistryService).pluginIDFor(eq(SYMBOLIC_NAME), anyString());
        verifyErrorsReported("Invocation of unload method [public int com.thoughtworks.go.plugin.activation.GoExtensionWithUnloadAnnotationMethodThrowingException"
                + ".throwExceptionAgain(com.thoughtworks.go.plugin.api.info.PluginContext) "
                + "throws java.io.IOException]. "
                + "Reason: java.io.IOException: Unload Dummy Checked Exception.");
    }

    @Test
    public void shouldRegisterServiceWithBundleSymbolicNamePluginIDAndExtensionTypeAsProperties() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);
        when(pluginRegistryService.pluginIDFor(eq(SYMBOLIC_NAME), anyString())).thenReturn(PLUGIN_ID);

        activator.start(context);

        assertActivatorHasNoErrors();
        verify(context).registerService(eq(GoPlugin.class), any(GoPlugin.class), propertiesCaptor.capture());
        assertThat(propertiesCaptor.getValue().toString())
            .isEqualTo(Map.of(Constants.BUNDLE_SYMBOLICNAME, PLUGIN_ID, Constants.BUNDLE_CATEGORY, "test-extension", "PLUGIN_ID", PLUGIN_ID).toString());
    }

    @Test
    public void shouldRegisterEachServiceWithTheCorrespondingPluginIDAsProperty() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class", "PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);
        when(bundle.loadClass("PublicGoExtensionClassWhichWillAlsoLoadSuccessfully")).thenReturn((Class) PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class);

        when(pluginRegistryService.pluginIDFor(SYMBOLIC_NAME, PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class.getCanonicalName())).thenReturn("plugin_id_1");
        when(pluginRegistryService.pluginIDFor(SYMBOLIC_NAME, PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class.getCanonicalName())).thenReturn("plugin_id_2");

        activator.start(context);

        assertActivatorHasNoErrors();
        verify(context).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class), propertiesCaptor.capture());
        assertThat(propertiesCaptor.getValue().toString())
            .isEqualTo(Map.of(Constants.BUNDLE_SYMBOLICNAME, SYMBOLIC_NAME, Constants.BUNDLE_CATEGORY, "test-extension", "PLUGIN_ID", "plugin_id_1").toString());
        verify(context).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class), propertiesCaptor.capture());
        assertThat(propertiesCaptor.getValue().toString())
            .isEqualTo(Map.of(Constants.BUNDLE_SYMBOLICNAME, SYMBOLIC_NAME, Constants.BUNDLE_CATEGORY, "test-extension-2", "PLUGIN_ID", "plugin_id_2").toString());
    }

    @Test
    public void shouldReportErrorsIfAPluginIDCannotBeFoundForAGivenExtensionClass() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class", "PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);
        when(bundle.loadClass("PublicGoExtensionClassWhichWillAlsoLoadSuccessfully")).thenReturn((Class) PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class);

        when(pluginRegistryService.pluginIDFor(SYMBOLIC_NAME, PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class.getCanonicalName())).thenReturn("plugin_id_1");
        when(pluginRegistryService.pluginIDFor(SYMBOLIC_NAME, PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class.getCanonicalName())).thenReturn(null);

        activator.start(context);

        verify(pluginRegistryService, times(2)).pluginIDFor(eq(SYMBOLIC_NAME), any());
        verify(context).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class), propertiesCaptor.capture());
        assertThat(propertiesCaptor.getValue().toString())
            .isEqualTo(Map.of(Constants.BUNDLE_SYMBOLICNAME, SYMBOLIC_NAME, Constants.BUNDLE_CATEGORY, "test-extension", "PLUGIN_ID", "plugin_id_1").toString());
        verify(context, never()).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class), any());

        assertThat(activator.hasErrors()).isEqualTo(true);
        verifyErrorsReported("Unable to find plugin ID for extension class (com.thoughtworks.go.plugin.activation.PublicGoExtensionClassWhichWillAlsoLoadSuccessfully) in bundle plugin-id");
    }

    @Test
    public void shouldFailToRegisterServiceWhenExtensionTypeCannotBeSuccessfullyRetrieved() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier.class);

        activator.start(context);

        assertActivatorHasErrors()
            .anyMatch(s -> s.contains("Unable to find extension type from plugin identifier in class com.thoughtworks.go.plugin.activation.PublicGoExtensionClassWhichWillLoadSuccessfullyButThrowWhenAskedForPluginIdentifier"));
        verify(context, times(0)).registerService(eq(GoPlugin.class), any(GoPlugin.class), any());
    }

    @Test
    public void shouldLoadOnlyRegisteredExtensionClassesWhenAMultiPluginBundleIsUsed() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class", "PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);
        when(bundle.loadClass("PublicGoExtensionClassWhichWillAlsoLoadSuccessfully")).thenReturn((Class) PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class);

        when(pluginRegistryService.pluginIDFor(SYMBOLIC_NAME, PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class.getCanonicalName())).thenReturn("plugin_id_1");
        when(pluginRegistryService.extensionClassesInBundle(SYMBOLIC_NAME)).thenReturn(List.of(PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class.getCanonicalName()));

        activator.start(context);

        assertActivatorHasNoErrors();
        verify(context, times(1)).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class), any());
        verify(context, never()).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillAlsoLoadSuccessfully.class), any());
        verify(bundle, never()).loadClass("PublicGoExtensionClassWhichWillAlsoLoadSuccessfully");
    }

    @Test
    public void shouldReportErrorsIfARegisteredExtensionClassCannotBeFound() throws Exception {
        setupClassesInBundle("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class");
        when(bundle.loadClass("PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier")).thenReturn((Class) PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class);

        when(pluginRegistryService.extensionClassesInBundle(SYMBOLIC_NAME)).thenReturn(List.of("com.some.InvalidClass"));

        activator.start(context);

        assertThat(activator.hasErrors()).isEqualTo(true);
        verify(context, never()).registerService(eq(GoPlugin.class), any(PublicGoExtensionClassWhichWillLoadSuccessfullyAndProvideAValidIdentifier.class), any());
        verify(pluginRegistryService).reportErrorAndInvalidate(SYMBOLIC_NAME, List.of("Extension class declared in plugin bundle is not found: com.some.InvalidClass"));
    }

    private void verifyThatOneOfTheErrorMessagesIsPresent(String... expectedErrorMessages) {
        verify(pluginRegistryService).getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME);
        verify(pluginRegistryService).reportErrorAndInvalidate(eq(PLUGIN_ID), errorMessageCaptor.capture());
        verify(pluginRegistryService).extensionClassesInBundle(SYMBOLIC_NAME);
        verifyNoMoreInteractions(pluginRegistryService);

        assertThat(activator.hasErrors()).isEqualTo(true);
        assertThat(errorMessageCaptor.getValue())
            .first()
            .satisfies(e -> assertThat(expectedErrorMessages).contains(e));
    }

    private void setupClassesInBundle(String... classes) {
        Collection<URL> urls = Arrays.stream(classes).map(c -> UrlUtil.fromFile(new File("/" + c))).toList();
        when(bundle.findEntries("/", "*.class", true)).thenReturn(Collections.enumeration(urls));
    }

    private void verifyErrorsReported(String... errors) {
        verify(pluginRegistryService).getPluginIDOfFirstPluginInBundle(SYMBOLIC_NAME);
        verify(pluginRegistryService).reportErrorAndInvalidate(PLUGIN_ID, List.of(errors));
        verify(pluginRegistryService).extensionClassesInBundle(SYMBOLIC_NAME);
        verifyNoMoreInteractions(pluginRegistryService);
    }

    private ListAssert<String> assertActivatorHasErrors() {
        assertThat(activator.hasErrors()).isEqualTo(true);
        verify(pluginRegistryService).reportErrorAndInvalidate(eq(PLUGIN_ID), errorMessageCaptor.capture());
        List<String> errors = errorMessageCaptor.getValue();
        return assertThat(errors);
    }

    private void assertActivatorHasNoErrors() {
        if (activator.hasErrors()) {
            verify(pluginRegistryService).reportErrorAndInvalidate(eq(PLUGIN_ID), errorMessageCaptor.capture());
            assertThat(errorMessageCaptor.getValue()).isEmpty();
        }
    }

    @Extension
    public abstract class PublicAbstractGoExtensionClass {
    }

    @Extension
    public static class PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor implements GoPlugin {
        @SuppressWarnings("unused")
        public PublicGoExtensionClassWhichDoesNotHaveADefaultConstructor(int x) {
        }

        @Override
        public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        }

        @Override
        public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) {
            return null;
        }

        @Override
        public GoPluginIdentifier pluginIdentifier() {
            return null;
        }
    }

    @Extension
    public static class PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor implements GoPlugin {
        public PublicGoExtensionClassWhichThrowsAnExceptionInItsConstructor() {
            throw new RuntimeException(CONSTRUCTOR_FAIL_MSG);
        }

        @Override
        public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        }

        @Override
        public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) {
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
