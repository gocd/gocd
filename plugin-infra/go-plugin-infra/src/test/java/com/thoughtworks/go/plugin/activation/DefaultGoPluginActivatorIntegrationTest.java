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

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.plugin.activation.test.*;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.TestGoPluginExtensionPoint;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.FelixGoPluginOSGiFramework;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import lib.test.DummyTestPluginInLibDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.framework.util.FelixConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DefaultGoPluginActivatorIntegrationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File tmpDir;
    private static final String BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR = "DefaultGoPluginActivatorIntegrationTest.bundleDirWhichHasProperActivator";
    private static final String NO_EXT_ERR_MSG = "No extensions found in this plugin.Please check for @Extension annotations";
    private static final String GO_TEST_DUMMY_SYMBOLIC_NAME = "Go-Test-Dummy-Symbolic-Name";

    private FelixGoPluginOSGiFramework framework;
    private StubOfDefaultPluginRegistry registry;

    @Before
    public void setUp() throws IOException {
        tmpDir = temporaryFolder.newFolder();
        registry = new StubOfDefaultPluginRegistry();
        framework = new FelixGoPluginOSGiFramework(registry, new SystemEnvironment()) {
            @Override
            protected HashMap<String, String> generateOSGiFrameworkConfig() {
                HashMap<String, String> config = super.generateOSGiFrameworkConfig();
                config.put(FelixConstants.RESOLVER_PARALLELISM, "1");
                return config;
            }
        };
        framework.start();
    }

    @Test
    public void shouldRegisterAClassImplementingGoPluginAsAnOSGiService() throws Exception {
        assertThatPluginWithThisExtensionClassLoadsSuccessfully(DummyTestPlugin.class);
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassImplementingGoPluginWithoutAPublicConstructor() throws Exception {
        installBundleWithClasses(DummyTestPluginWithNonPublicDefaultConstructor.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassImplementingGoPluginWithOnlyAOneArgConstructor() throws Exception {
        installBundleWithClasses(DummyGoPluginWithOneArgConstructorOnly.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        String error = descriptor.getStatus().getMessages().get(0);
        assertThat(error.contains("DummyGoPluginWithOneArgConstructorOnly"), is(true));
        assertThat(error.contains("Make sure it and all of its parent classes have a default constructor."), is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAnExtensionClassWhichDoesNotImplementAGoExtensionPoint() throws Exception {
        installBundleWithClasses(NotAGoExtensionPoint.class, NotAGoExtensionAsItDoesNotImplementAnyExtensionPoints.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotLoadClassesFoundInMETA_INFEvenIfTheyAreProperGoExtensionPoints() throws Exception {
        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, DummyTestPlugin.class);
        File sourceClassFile = new File(bundleWithActivator, "com/thoughtworks/go/plugin/activation/test/DummyTestPlugin.class");
        File destinationFile = new File(bundleWithActivator, "META-INF/com/thoughtworks/go/plugin/activation/test/");
        FileUtils.moveFileToDirectory(sourceClassFile, destinationFile, true);

        installBundleFoundInDirectory(bundleWithActivator);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotFailToRegisterOtherClassesIfAClassCannotBeLoadedBecauseOfWrongPath() throws Exception {
        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, DummyTestPlugin.class);
        File sourceClassFile = new File(bundleWithActivator, "com/thoughtworks/go/plugin/activation/test/DummyTestPlugin.class");
        File destinationFile = new File(bundleWithActivator, "ABC-DEF/com/thoughtworks/go/plugin/activation/test/");
        FileUtils.copyFileToDirectory(sourceClassFile, destinationFile, true);

        Bundle bundle = installBundleFoundInDirectory(bundleWithActivator);
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
    }

    @Test
    public void shouldNotLoadAClassFoundInLibDirectoryEvenIfItIsAProperGoExtensionPoints() throws Exception {
        installBundleWithClasses(DummyTestPluginInLibDirectory.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichIsAbstract() throws Exception {
        installBundleWithClasses(AbstractTestPlugin.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichIsNotPublic() throws Exception {
        installBundleWithClasses(DummyTestPluginWhichIsNotPublic.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAnInterfaceEvenIfItImplementsAGoExtensionPointInterface() throws Exception {
        installBundleWithClasses(TestGoPluginExtensionInterface.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichThrowsExceptionDuringInstantiation() throws Exception {
        installBundleWithClasses(DummyTestPlugin.class, DummyGoPluginWhichThrowsAnExceptionDuringConstruction.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        String error = descriptor.getStatus().getMessages().get(0);
        assertThat(error.contains("DummyGoPluginWhichThrowsAnExceptionDuringConstruction"), is(true));
        assertThat(error.contains("java.lang.RuntimeException: Ouch! I failed!"), is(true));
    }

    @Test
    public void shouldRegisterANestedClassImplementingGoPluginAsAnOSGiService() throws Exception {
        if (new OSChecker(OSChecker.WINDOWS).satisfy()) {
            return; // The class files in this test become too big for a Windows filesystem to handle.
        }

        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, TestPluginOuterClass.class,
                TestPluginOuterClass.NestedClass.class,
                TestPluginOuterClass.InnerClass.class,
                TestPluginOuterClass.InnerClass.SecondLevelInnerClass.class,
                TestPluginOuterClass.InnerClass.SecondLevelInnerClass.TestPluginThirdLevelInnerClass.class,
                TestPluginOuterClass.InnerClass.SecondLevelSiblingInnerClassNoDefaultConstructor.class);
        BundleContext installedBundledContext = bundleContext(installBundleFoundInDirectory(bundleWithActivator));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(GoPlugin.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 4, services.length);
        assertEquals(TestPluginOuterClass.class.getName(), services[0]);
        assertEquals(TestPluginOuterClass.InnerClass.class.getName(), services[1]);
        assertEquals(TestPluginOuterClass.InnerClass.SecondLevelInnerClass.TestPluginThirdLevelInnerClass.class.getName(), services[2]);
        assertEquals(TestPluginOuterClass.NestedClass.class.getName(), services[3]);
    }

    @Test
    public void shouldRegisterAsAnOSGiServiceADerivedClassWhoseAncestorImplementsAnExtensionPoint() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(TestPluginThatIsADerivedClass.class,
                DummyTestPlugin.class, TestPluginThatIsADerivedClass.class.getSuperclass()));
        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(GoPlugin.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 2, services.length);
        assertEquals(DummyTestPlugin.class.getName(), services[0]);
        assertEquals(TestPluginThatIsADerivedClass.class.getName(), services[1]);
    }

    @Test
    public void shouldRegisterOneInstanceForEachExtensionPointAnExtensionImplements() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class, DummyTestPlugin.class));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(GoPlugin.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 2, services.length);
        assertEquals(DummyTestPlugin.class.getName(), services[0]);
        assertEquals(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class.getName(), services[1]);

        references = installedBundledContext.getServiceReferences(TestGoPluginExtensionPoint.class.getName(), null);
        assertEquals(1, references.length);
        assertEquals(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class.getName(), installedBundledContext.getService(references[0]).getClass().getName());
        Object testExtensionImplementation = getImplementationOfType(installedBundledContext, references, TestGoPluginExtensionThatImplementsTwoExtensionPoints.class);

        references = installedBundledContext.getServiceReferences(GoPlugin.class.getName(), null);
        assertEquals(2, references.length);
        Object testPluginImplementation = getImplementationOfType(installedBundledContext, references, TestGoPluginExtensionThatImplementsTwoExtensionPoints.class);

        assertSame(testExtensionImplementation, testPluginImplementation);
    }

    @Test
    public void shouldRegisterOneInstanceForEachExtensionPointWhereThePluginClassExtendsABaseClassWhichIsAnExtensionAndImplementsAGoExtensionPoint() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(ClassThatExtendsTestExtensionPoint.class,
                ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class, TestGoPluginExtensionPoint.class));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(TestGoPluginExtensionPoint.class.getName(), null);
        assertEquals(1, references.length);
        Object testExtensionImplementation = getImplementationOfType(installedBundledContext, references, ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class);

        references = installedBundledContext.getServiceReferences(GoPlugin.class.getName(), null);
        assertEquals(1, references.length);
        Object testPluginImplementation = getImplementationOfType(installedBundledContext, references, ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class);

        assertSame(testExtensionImplementation, testPluginImplementation);
    }

    @Test
    public void shouldNotRegisterAnAnonymousClassThatImplementsAnExtensionPoint() throws IOException {
        installBundleWithClasses(DummyClassProvidingAnonymousClass.getAnonymousClass().getClass());

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterAnAnonymousClassDefinedWithinAnInnerClassThatImplementsAnExtensionPoint() throws IOException {
        installBundleWithClasses(DummyClassProvidingAnonymousClass.DummyInnerClassProvidingAnonymousClass.getAnonymousClass().getClass());

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterLocalInnerClassesThatImplementAnExtensionPoint() throws IOException {
        installBundleWithClasses(DummyClassWithLocalInnerClass.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterPublicInnerClassesThatImplementAnExtensionPointInsidePackageLevelClass() throws IOException {
        installBundleWithClasses(PackageLevelClassWithPublicInnerClass.class, PackageLevelClassWithPublicInnerClass.DummyInnerClassWithExtension.class);

        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldBeAbleToUsePackagesFromJavaxWithinThePluginSinceItHasBeenExportedUsingBootDelegationInTheOSGIFramework() throws Exception {
        assertThatPluginWithThisExtensionClassLoadsSuccessfully(ClassWhichUsesSomeClassInJavaxPackage.class);
    }

    @Test
    public void shouldBeAbleToUsePackagesFromOrgXmlSaxPackageWithinThePluginSinceItHasBeenExportedUsingBootDelegationInTheOSGIFramework() throws Exception {
        assertThatPluginWithThisExtensionClassLoadsSuccessfully(ClassWhichUsesSomeClassesInOrgXMLSaxPackage.class);
    }

    @Test
    public void shouldBeAbleToUsePackagesFromOrgW3cDomPackageWithinThePluginSinceItHasBeenExportedUsingBootDelegationInTheOSGIFramework() throws Exception {
        assertThatPluginWithThisExtensionClassLoadsSuccessfully(ClassWhichUsesSomeClassesInOrgW3CDomPackage.class);
    }

    @After
    public void tearDown() {
        framework.stop();
    }

    private void assertThatPluginWithThisExtensionClassLoadsSuccessfully(Class<?> extensionClass) throws IOException, InvalidSyntaxException {
        BundleContext installedBundleContext = bundleContext(installBundleWithClasses(extensionClass));

        ServiceReference<?>[] references = installedBundleContext.getServiceReferences(GoPlugin.class.getName(), null);
        assertEquals("No service registered for GoPlugin class", 1, references.length);
        assertEquals("Symbolic Name property should be present", GO_TEST_DUMMY_SYMBOLIC_NAME, references[0].getProperty(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals(extensionClass.getName(), installedBundleContext.getService(references[0]).getClass().getName());
    }

    private String[] toSortedServiceClassNames(BundleContext installedBundledContext, ServiceReference<?>[] references) {
        if (references == null) {
            return new String[0];
        }

        String[] services = new String[references.length];
        for (int i = 0; i < references.length; i++) {
            ServiceReference<?> reference = references[i];
            services[i] = installedBundledContext.getService(reference).getClass().getName();
        }
        Arrays.sort(services);
        return services;
    }

    private Object getImplementationOfType(BundleContext installedBundledContext, ServiceReference<?>[] references, Class<?> type) {
        if (references == null) {
            return new String[0];
        }

        for (ServiceReference<?> reference : references) {
            Object service = installedBundledContext.getService(reference);
            if (service.getClass().getName().equals(type.getName())) {
                return service;
            }
        }

        throw new RuntimeException("Class type not found: " + type);
    }

    private Bundle installBundleWithClasses(Class... classesToBeLoaded) throws IOException {
        return installBundleFoundInDirectory(createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, classesToBeLoaded));
    }

    private Bundle installBundleFoundInDirectory(File bundleWithActivator) {
        final GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(GO_TEST_DUMMY_SYMBOLIC_NAME, "1", null, null, bundleWithActivator, true);
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        registry.fakeRegistrationOfPlugin(pluginDescriptor);
        return framework.loadPlugin(bundleDescriptor);
    }

    private BundleContext bundleContext(Bundle bundle) {
        return bundle.getBundleContext();
    }

    private File createBundleWithActivator(String destinationDir, Class... classesToBeAdded) throws IOException {
        TinyBundle bundleBeingBuilt = TinyBundles.bundle()
                .add(GoPluginActivator.class)
                .add(DefaultGoPluginActivator.class, InnerClassStrategy.ALL)
                .set(Constants.BUNDLE_ACTIVATOR, DefaultGoPluginActivator.class.getCanonicalName())
                .set(Constants.BUNDLE_CLASSPATH, ".,lib/dependency.jar")
                .set(Constants.BUNDLE_SYMBOLICNAME, GO_TEST_DUMMY_SYMBOLIC_NAME);
        for (Class aClass : classesToBeAdded) {
            bundleBeingBuilt.add(aClass, InnerClassStrategy.NONE);
        }
        ZipInputStream src = new ZipInputStream(bundleBeingBuilt.build());
        File bundleExplodedDir = explodeBundleIntoDirectory(src, destinationDir);
        IOUtils.closeQuietly(src);
        return bundleExplodedDir;
    }

    private File explodeBundleIntoDirectory(ZipInputStream src, String destinationDir) throws IOException {
        File destinationPluginBundleLocation = new File(tmpDir, destinationDir);
        destinationPluginBundleLocation.mkdirs();
        new ZipUtil().unzip(src, destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }

    private class StubOfDefaultPluginRegistry extends DefaultPluginRegistry {
        void fakeRegistrationOfPlugin(GoPluginDescriptor pluginDescriptor) {
            idToDescriptorMap.putIfAbsent(pluginDescriptor.id().toLowerCase(), pluginDescriptor);
        }
    }
}

@Extension
class DummyTestPluginWhichIsNotPublic implements GoPlugin {
    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        throw new UnsupportedOperationException();
    }
}

class PackageLevelClassWithPublicInnerClass {
    @Extension
    public class DummyInnerClassWithExtension implements GoPlugin {
        @Override
        public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GoPluginIdentifier pluginIdentifier() {
            throw new UnsupportedOperationException();
        }
    }
}
