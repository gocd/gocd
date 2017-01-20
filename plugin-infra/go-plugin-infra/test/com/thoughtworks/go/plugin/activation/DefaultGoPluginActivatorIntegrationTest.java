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

package com.thoughtworks.go.plugin.activation;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.plugin.activation.test.*;
import com.thoughtworks.go.plugin.api.TestGoPluginExtensionPoint;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.infra.FelixGoPluginOSGiFramework;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import lib.test.DummyPluginAwareExtensionInLibDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DefaultGoPluginActivatorIntegrationTest {

    private static final File TMP_DIR = new File("./tmp");
    private static final String BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR = "DefaultGoPluginActivatorIntegrationTest.bundleDirWhichHasProperActivator";
    public static final String NO_EXT_ERR_MSG = "No extensions found in this plugin.Please check for @Extension annotations";
    public static final String GO_TEST_DUMMY_SYMBOLIC_NAME = "Go-Test-Dummy-Symbolic-Name";

    private FelixGoPluginOSGiFramework framework;
    private StubOfDefaultPluginRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new StubOfDefaultPluginRegistry();
        framework = new FelixGoPluginOSGiFramework(registry, new SystemEnvironment());
        framework.start();
    }

    @Test
    public void shouldRegisterAClassImplementingPluginDescriptorAwareAsAnOSGiService() throws Exception {
        assertThatPluginWithThisExtensionClassLoadsSuccessfully(DummyPluginAwareExtension.class);
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassImplementingPluginDescriptorAwareWithoutAPublicConstructor() throws Exception {
        Bundle bundle = installBundleWithClasses(DummyPluginAwareExtensionWithNonPublicDefaultConstructor.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassImplementingPluginDescriptorAwareWithOnlyAOneArgConstructor() throws Exception {
        Bundle bundle = installBundleWithClasses(DummyPluginAwareExtensionWithOneArgConstructorOnly.class);
        assertThat(bundle.getState(),is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        String error = descriptor.getStatus().getMessages().get(0);
        assertThat(error.contains("DummyPluginAwareExtensionWithOneArgConstructorOnly"),is(true));
        assertThat(error.contains("Make sure it and all of its parent classes have a default constructor."),is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAnExtensionClassWhichDoesNotImplementAGoExtensionPoint() throws Exception {
        Bundle bundle = installBundleWithClasses(NotAGoExtensionPoint.class, NotAGoExtensionAsItDoesNotImplementAnyExtensionPoints.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotLoadClassesFoundInMETA_INFEvenIfTheyAreProperGoExtensionPoints() throws Exception {
        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, DummyPluginAwareExtension.class);
        File sourceClassFile = new File(bundleWithActivator, "com/thoughtworks/go/plugin/activation/test/DummyPluginAwareExtension.class");
        File destinationFile = new File(bundleWithActivator, "META-INF/com/thoughtworks/go/plugin/activation/test/");
        FileUtils.moveFileToDirectory(sourceClassFile, destinationFile, true);

        Bundle bundle = installBundleFoundInDirectory(bundleWithActivator);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG),is(true));

    }

    @Test
    public void shouldNotFailToRegisterOtherClassesIfAClassCannotBeLoadedBecauseOfWrongPath() throws Exception {
        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, DummyPluginAwareExtension.class);
        File sourceClassFile = new File(bundleWithActivator, "com/thoughtworks/go/plugin/activation/test/DummyPluginAwareExtension.class");
        File destinationFile = new File(bundleWithActivator, "ABC-DEF/com/thoughtworks/go/plugin/activation/test/");
        FileUtils.copyFileToDirectory(sourceClassFile, destinationFile, true);

        Bundle bundle = installBundleFoundInDirectory(bundleWithActivator);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    @Test
    public void shouldNotLoadAClassFoundInLibDirectoryEvenIfItIsAProperGoExtensionPoints() throws Exception {
        Bundle bundle = installBundleWithClasses(DummyPluginAwareExtensionInLibDirectory.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichIsAbstract() throws Exception {
        Bundle bundle = installBundleWithClasses(AbstractPluginAwareExtension.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichIsNotPublic() throws Exception {
        Bundle bundle = installBundleWithClasses(DummyPluginAwareExtensionWhichIsNotPublic.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAnInterfaceEvenIfItImplementsAGoExtensionPointInterface() throws Exception {
        Bundle bundle = installBundleWithClasses(PluginAwareExtensionInterface.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));

    }

    @Test
    public void shouldNotRegisterAsAnOSGiServiceAClassWhichThrowsExceptionDuringInstantiation() throws Exception {
        Bundle bundle = installBundleWithClasses(DummyPluginAwareExtension.class, DummyPluginAwareExtensionWhichThrowsAnExceptionDuringConstruction.class);
        assertThat(bundle.getState(),is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        String error = descriptor.getStatus().getMessages().get(0);
        assertThat(error.contains("DummyPluginAwareExtensionWhichThrowsAnExceptionDuringConstruction"), is(true));
        assertThat(error.contains("java.lang.RuntimeException: Ouch! I failed!"), is(true));
    }

    @Test
    public void shouldRegisterANestedClassImplementingPluginDescriptorAwareAsAnOSGiService() throws Exception {
        if (new OSChecker(OSChecker.WINDOWS).satisfy()) {
            return; // The class files in this test become too big for a Windows filesystem to handle.
        }

        File bundleWithActivator = createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, PluginAwareExtensionOuterClass.class,
                PluginAwareExtensionOuterClass.NestedClass.class,
                PluginAwareExtensionOuterClass.InnerClass.class,
                PluginAwareExtensionOuterClass.InnerClass.SecondLevelInnerClass.class,
                PluginAwareExtensionOuterClass.InnerClass.SecondLevelInnerClass.PluginAwareExtensionThirdLevelInnerClass.class,
                PluginAwareExtensionOuterClass.InnerClass.SecondLevelSiblingInnerClassNoDefaultConstructor.class);
        BundleContext installedBundledContext = bundleContext(installBundleFoundInDirectory(bundleWithActivator));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 4, services.length);
        assertEquals(PluginAwareExtensionOuterClass.class.getName(), services[0]);
        assertEquals(PluginAwareExtensionOuterClass.InnerClass.class.getName(), services[1]);
        assertEquals(PluginAwareExtensionOuterClass.InnerClass.SecondLevelInnerClass.PluginAwareExtensionThirdLevelInnerClass.class.getName(), services[2]);
        assertEquals(PluginAwareExtensionOuterClass.NestedClass.class.getName(), services[3]);
    }

    @Test
    public void shouldRegisterAsAnOSGiServiceADerivedClassWhoseAncestorImplementsAnExtensionPoint() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(PluginAwareExtensionThatIsADerivedClass.class,
                DummyPluginAwareExtension.class, PluginAwareExtensionThatIsADerivedClass.class.getSuperclass()));
        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 2, services.length);
        assertEquals(DummyPluginAwareExtension.class.getName(), services[0]);
        assertEquals(PluginAwareExtensionThatIsADerivedClass.class.getName(), services[1]);
    }

    @Test
    public void shouldRegisterOneInstanceForEachExtensionPointAnExtensionImplements() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class,
                DummyPluginAwareExtension.class));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        String[] services = toSortedServiceClassNames(installedBundledContext, references);

        assertEquals(Arrays.toString(services), 2, services.length);
        assertEquals(DummyPluginAwareExtension.class.getName(), services[0]);
        assertEquals(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class.getName(), services[1]);

        references = installedBundledContext.getServiceReferences(TestGoPluginExtensionPoint.class.getName(), null);
        assertEquals(1, references.length);
        assertEquals(TestGoPluginExtensionThatImplementsTwoExtensionPoints.class.getName(), installedBundledContext.getService(references[0]).getClass().getName());
        Object testExtensionImplementation = getImplementationOfType(installedBundledContext, references, TestGoPluginExtensionThatImplementsTwoExtensionPoints.class);

        references = installedBundledContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        assertEquals(2, references.length);
        Object descriptorAwareImplementation = getImplementationOfType(installedBundledContext, references, TestGoPluginExtensionThatImplementsTwoExtensionPoints.class);

        assertSame(testExtensionImplementation, descriptorAwareImplementation);
    }

    @Test
    public void shouldRegisterOneInstanceForEachExtensionPointWhereThePluginClassExtendsABaseClassWhichIsAnExtensionAndImplementsAGoExtensionPoint() throws Exception {
        BundleContext installedBundledContext = bundleContext(installBundleWithClasses(ClassThatExtendsTestExtensionPoint.class,
                ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class, TestGoPluginExtensionPoint.class));

        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(TestGoPluginExtensionPoint.class.getName(), null);
        assertEquals(1, references.length);
        Object testExtensionImplementation = getImplementationOfType(installedBundledContext, references, ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class);

        references = installedBundledContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        assertEquals(1, references.length);
        Object descriptorAwareImplementation = getImplementationOfType(installedBundledContext, references, ClassThatExtendsTestExtensionPoint.ClassThatExtendsTwoGoExtensionPoint.class);

        assertSame(testExtensionImplementation, descriptorAwareImplementation);
    }

    @Test
    public void shouldNotRegisterAnAnonymousClassThatImplementsAnExtensionPoint() throws BundleException, IOException, URISyntaxException, InvalidSyntaxException {
        Bundle bundle = installBundleWithClasses(DummyClassProvidingAnonymousClass.getAnonymousClass().getClass());
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG),is(true));
    }

    @Test
    public void shouldNotRegisterAnAnonymousClassDefinedWithinAnInnerClassThatImplementsAnExtensionPoint() throws BundleException, IOException, URISyntaxException, InvalidSyntaxException {
        Bundle bundle = installBundleWithClasses(DummyClassProvidingAnonymousClass.DummyInnerClassProvidingAnonymousClass.getAnonymousClass().getClass());
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterLocalInnerClassesThatImplementAnExtensionPoint() throws BundleException, IOException, URISyntaxException, InvalidSyntaxException {
        Bundle bundle = installBundleWithClasses(DummyClassWithLocalInnerClass.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        GoPluginDescriptor descriptor = registry.getPlugin(GO_TEST_DUMMY_SYMBOLIC_NAME);
        assertThat(descriptor.isInvalid(), is(true));
        assertThat(descriptor.getStatus().getMessages().contains(NO_EXT_ERR_MSG), is(true));
    }

    @Test
    public void shouldNotRegisterPublicInnerClassesThatImplementAnExtensionPointInsidePackageLevelClass() throws BundleException, IOException, URISyntaxException, InvalidSyntaxException {
        Bundle bundle = installBundleWithClasses(PackageLevelClassWithPublicInnerClass.class, PackageLevelClassWithPublicInnerClass.DummyInnerClassWithExtension.class);
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
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
    public void tearDown() throws Exception {
        framework.stop();
        FileUtils.deleteDirectory(TMP_DIR);
    }

    private void assertThatPluginWithThisExtensionClassLoadsSuccessfully(Class<?> extensionClass) throws IOException, URISyntaxException, BundleException, InvalidSyntaxException {
        BundleContext installedBundleContext = bundleContext(installBundleWithClasses(extensionClass));

        ServiceReference<?>[] references = installedBundleContext.getServiceReferences(PluginDescriptorAware.class.getName(), null);
        assertEquals("No service registered for PluginDescriptorAware class", 1, references.length);
        assertEquals("Symbolic Name property should be present", GO_TEST_DUMMY_SYMBOLIC_NAME, references[0].getProperty(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals(extensionClass.getName(), installedBundleContext.getService(references[0]).getClass().getName());
    }

    private void assertNoReferencesFor(Class<?> clazz, BundleContext installedBundledContext) throws InvalidSyntaxException {
        ServiceReference<?>[] references = installedBundledContext.getServiceReferences(clazz.getName(), null);
        assertNull("Found references: " + Arrays.toString(references), references);
    }

    private void verifyActivatorHasErrors(Bundle bundle) {
        Object m_activator = ReflectionUtil.getField(bundle, "m_activator");
        try {
            Object hasErrors = ReflectionUtil.invoke(m_activator, "hasErrors");
            assertThat(hasErrors, Is.is(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private Bundle installBundleWithClasses(Class... classesToBeLoaded) throws IOException, URISyntaxException, BundleException {
        return installBundleFoundInDirectory(createBundleWithActivator(BUNDLE_DIR_WHICH_HAS_PROPER_ACTIVATOR, classesToBeLoaded));
    }

    private Bundle installBundleFoundInDirectory(File bundleWithActivator) throws BundleException {
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(GO_TEST_DUMMY_SYMBOLIC_NAME, "1", null, null, bundleWithActivator, true);
        registry.fakeRegistrationOfPlugin(pluginDescriptor);
        return framework.loadPlugin(pluginDescriptor);
    }

    private BundleContext bundleContext(Bundle bundle){
        return bundle.getBundleContext();
    }

    private File createBundleWithActivator(String destinationDir, Class... classesToBeAdded) throws IOException, URISyntaxException {
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

    private File explodeBundleIntoDirectory(ZipInputStream src, String destinationDir) throws IOException, URISyntaxException {
        File destinationPluginBundleLocation = new File(TMP_DIR, destinationDir);
        destinationPluginBundleLocation.mkdirs();
        new ZipUtil().unzip(src, destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }

    private class StubOfDefaultPluginRegistry extends DefaultPluginRegistry {
        public void fakeRegistrationOfPlugin(GoPluginDescriptor pluginDescriptor) {
            idToDescriptorMap.putIfAbsent(pluginDescriptor.id(), pluginDescriptor);
        }
    }
}

@Extension
class DummyPluginAwareExtensionWhichIsNotPublic implements PluginDescriptorAware {
    @Override
    public void setPluginDescriptor(PluginDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }
}

class PackageLevelClassWithPublicInnerClass {
    @Extension
    public class DummyInnerClassWithExtension implements PluginDescriptorAware {
        @Override
        public void setPluginDescriptor(PluginDescriptor descriptor) {
            throw new UnsupportedOperationException();
        }
    }
}