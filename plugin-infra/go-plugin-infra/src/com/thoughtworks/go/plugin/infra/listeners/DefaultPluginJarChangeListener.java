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

package com.thoughtworks.go.plugin.infra.listeners;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ExceptionHandler;
import com.thoughtworks.go.plugin.infra.GoPluginOSGiFramework;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptorBuilder;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginOSGiManifest;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginOSGiManifestGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;

@Component
public class DefaultPluginJarChangeListener implements PluginJarChangeListener {
    private static final String ACTIVATOR_JAR_NAME = GoPluginOSGiManifest.ACTIVATOR_JAR_NAME;
    private static Logger LOGGER = Logger.getLogger(DefaultPluginJarChangeListener.class);
    private final DefaultPluginRegistry registry;
    private final GoPluginOSGiManifestGenerator osgiManifestGenerator;
    private final GoPluginOSGiFramework goPluginOSGiFramework;
    private final SystemEnvironment systemEnvironment;
    private GoPluginDescriptorBuilder goPluginDescriptorBuilder;

    @Autowired
    public DefaultPluginJarChangeListener(DefaultPluginRegistry registry, GoPluginOSGiManifestGenerator osgiManifestGenerator, GoPluginOSGiFramework goPluginOSGiFramework,
                                          GoPluginDescriptorBuilder goPluginDescriptorBuilder, SystemEnvironment systemEnvironment) {
        this.registry = registry;
        this.osgiManifestGenerator = osgiManifestGenerator;
        this.goPluginOSGiFramework = goPluginOSGiFramework;
        this.goPluginDescriptorBuilder = goPluginDescriptorBuilder;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void pluginJarAdded(PluginFileDetails pluginFileDetails) {
        GoPluginDescriptor descriptor = goPluginDescriptorBuilder.build(pluginFileDetails.file(), pluginFileDetails.isBundledPlugin());
        GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(descriptor.id(), descriptor.fileName());
        validateIfExternalPluginRemovingBundledPlugin(descriptor, existingDescriptor);
        validatePluginCompatibilityWithCurrentOS(descriptor);
        addPlugin(pluginFileDetails, descriptor);
    }

    @Override
    public void pluginJarUpdated(PluginFileDetails pluginFileDetails) {
        GoPluginDescriptor descriptor = goPluginDescriptorBuilder.build(pluginFileDetails.file(), pluginFileDetails.isBundledPlugin());
        GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(descriptor.id(), descriptor.fileName());
        validateIfExternalPluginRemovingBundledPlugin(descriptor, existingDescriptor);
        validateIfSamePluginUpdated(descriptor, existingDescriptor);
        validatePluginCompatibilityWithCurrentOS(descriptor);
        removePlugin(descriptor);
        addPlugin(pluginFileDetails, descriptor);
    }

    @Override
    public void pluginJarRemoved(PluginFileDetails pluginFileDetails) {
        GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(null, pluginFileDetails.file().getName());
        if(existingDescriptor==null){
            return;
        }
        boolean externalPlugin = !pluginFileDetails.isBundledPlugin();
        boolean bundledPlugin = existingDescriptor.isBundledPlugin();
        boolean externalPluginWithSameIdAsBundledPlugin = bundledPlugin && externalPlugin;
        if (externalPluginWithSameIdAsBundledPlugin) {
            LOGGER.info(
                    String.format("External Plugin file '%s' having same name as bundled plugin file has been removed. "
                            + "Refusing to unload bundled plugin with id: '%s'",
                            pluginFileDetails.file(), existingDescriptor.id()));
            return;
        }
        removePlugin(existingDescriptor);
    }

    private void addPlugin(PluginFileDetails pluginFileDetails, GoPluginDescriptor descriptor) {
        explodePluginJarToBundleDir(pluginFileDetails.file(), descriptor.bundleLocation());
        installActivatorJarToBundleDir(descriptor.bundleLocation());
        registry.loadPlugin(descriptor);
        refreshBundle(descriptor);
    }

    private void removePlugin(GoPluginDescriptor descriptor) {
        GoPluginDescriptor descriptorOfRemovedPlugin = registry.unloadPlugin(descriptor);
        goPluginOSGiFramework.unloadPlugin(descriptorOfRemovedPlugin);
        boolean bundleLocationHasBeenDeleted = FileUtils.deleteQuietly(descriptorOfRemovedPlugin.bundleLocation());
        if (!bundleLocationHasBeenDeleted) {
            throw new RuntimeException(String.format("Failed to remove bundle jar %s from bundle location %s", descriptorOfRemovedPlugin.fileName(), descriptorOfRemovedPlugin.bundleLocation()));
        }
    }

    private void validateIfExternalPluginRemovingBundledPlugin(PluginFileDetails pluginFileDetails, GoPluginDescriptor existingDescriptor) {
        if (existingDescriptor != null && existingDescriptor.isBundledPlugin() && !pluginFileDetails.isBundledPlugin()) {
            throw new RuntimeException(String.format("Found bundled plugin with ID: [%s], external plugin could not be loaded", existingDescriptor.id()));
        }
    }

    private void validateIfExternalPluginRemovingBundledPlugin(GoPluginDescriptor newDescriptor, GoPluginDescriptor existingDescriptor) {
        if (existingDescriptor != null && existingDescriptor.isBundledPlugin() && !newDescriptor.isBundledPlugin()) {
            throw new RuntimeException(String.format("Found bundled plugin with ID: [%s], external plugin could not be loaded", existingDescriptor.id()));
        }
    }

    private void validateIfSamePluginUpdated(GoPluginDescriptor descriptor, GoPluginDescriptor existingDescriptor) {
        if (existingDescriptor != null && !existingDescriptor.fileName().equals(descriptor.fileName())) {
            throw new RuntimeException("Found another plugin with ID: " + existingDescriptor.id());
        }
    }

    private void validatePluginCompatibilityWithCurrentOS(GoPluginDescriptor descriptor) {
        String currentOS = systemEnvironment.getOperatingSystemFamilyName();

        if (!descriptor.isCurrentOSValidForThisPlugin(currentOS)) {
            List<String> messages = Arrays.asList(String.format("Plugin with ID (%s) is not valid: Incompatible with current operating system '%s'. Valid operating systems are: %s.",
                    descriptor.id(), currentOS, descriptor.about().targetOperatingSystems()));
            descriptor.markAsInvalid(messages, null);
        }
    }

    private void refreshBundle(GoPluginDescriptor descriptor) {
        if (!descriptor.isInvalid()) {
            osgiManifestGenerator.updateManifestOf(descriptor);
            goPluginOSGiFramework.loadPlugin(descriptor);
            providePluginDescriptorToThePlugin(descriptor);
        }
    }

    private void providePluginDescriptorToThePlugin(final GoPluginDescriptor descriptor) {
        if (descriptor.isInvalid()) {
            LOGGER.debug("Descriptor Invalid skipping plugin descriptor callback.");
            return;
        }
        if (!goPluginOSGiFramework.hasReferenceFor(PluginDescriptorAware.class, descriptor.id())) {
            return;
        }
        goPluginOSGiFramework.doOnAllWithExceptionHandlingForPlugin(
                PluginDescriptorAware.class, descriptor.id(), new Action<PluginDescriptorAware>() {
                    @Override
                    public void execute(PluginDescriptorAware descriptorAwarePlugin, GoPluginDescriptor pluginDescriptor) {
                        descriptorAwarePlugin.setPluginDescriptor(pluginDescriptor);
                    }
                }, new ExceptionHandler<PluginDescriptorAware>() {
                    @Override
                    public void handleException(PluginDescriptorAware obj, Throwable t) {
                        LOGGER.warn("Set Plugin Descriptor Call failed for plugin: " + descriptor.id(),t);
                    }
                }
        );
    }

    void explodePluginJarToBundleDir(File file, File location) {
        try {
            wipePluginBundleDirectory(location);
            ZipUtil zipUtil = new ZipUtil();
            zipUtil.unzip(file, location);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy plugin jar %s to bundle location %s", file, location), e);
        }
    }

    void installActivatorJarToBundleDir(File pluginBundleExplodedDir) {
        URL activatorJar = findAndValidateActivatorJar();
        File pluginActivatorJarDestination = new File(new File(pluginBundleExplodedDir, GoPluginOSGiManifest.PLUGIN_DEPENDENCY_DIR), ACTIVATOR_JAR_NAME);

        try {
            FileUtils.copyURLToFile(activatorJar, pluginActivatorJarDestination);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy activator jar %s to bundle dependency dir: %s", activatorJar, pluginActivatorJarDestination), e);
        }
    }

    private void wipePluginBundleDirectory(File pluginBundleDirectory) {
        if (pluginBundleDirectory.exists() && !FileUtils.deleteQuietly(pluginBundleDirectory)) {
            throw new RuntimeException(String.format("Failed to delete bundle directory %s", pluginBundleDirectory));
        }
        pluginBundleDirectory.mkdirs();
    }

    private URL findAndValidateActivatorJar() {
        URL activatorJar = getClass().getClassLoader().getResource(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH));
        if (activatorJar == null) {
            throw new RuntimeException("Unable to load plugins. Cannot find activator jar in classpath.");
        }
        return activatorJar;
    }

}
