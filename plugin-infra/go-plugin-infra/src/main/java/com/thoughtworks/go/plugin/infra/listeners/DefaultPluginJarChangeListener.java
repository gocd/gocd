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
package com.thoughtworks.go.plugin.infra.listeners;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.infra.PluginLoader;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.plugininfo.*;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static java.util.Collections.singletonList;

@Component
public class DefaultPluginJarChangeListener implements PluginJarChangeListener {
    private static final String ACTIVATOR_JAR_NAME = GoPluginOSGiManifest.ACTIVATOR_JAR_NAME;
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultPluginJarChangeListener.class);
    private final DefaultPluginRegistry registry;
    private final GoPluginOSGiManifestGenerator osgiManifestGenerator;
    private final PluginLoader pluginLoader;
    private final SystemEnvironment systemEnvironment;
    private GoPluginBundleDescriptorBuilder goPluginBundleDescriptorBuilder;

    @Autowired
    public DefaultPluginJarChangeListener(DefaultPluginRegistry registry, GoPluginOSGiManifestGenerator osgiManifestGenerator, PluginLoader pluginLoader, GoPluginBundleDescriptorBuilder goPluginBundleDescriptorBuilder, SystemEnvironment systemEnvironment) {
        this.registry = registry;
        this.osgiManifestGenerator = osgiManifestGenerator;
        this.pluginLoader = pluginLoader;
        this.goPluginBundleDescriptorBuilder = goPluginBundleDescriptorBuilder;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void pluginJarAdded(PluginFileDetails pluginFileDetails) {
        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginFileDetails.file(), pluginFileDetails.isBundledPlugin());

        validateIfExternalPluginRemovingBundledPlugin(bundleDescriptor);
        validatePluginCompatibilityWithCurrentOS(bundleDescriptor);
        validatePluginCompatibilityWithGoCD(bundleDescriptor);
        addPlugin(pluginFileDetails, bundleDescriptor);
    }

    @Override
    public void pluginJarUpdated(PluginFileDetails pluginFileDetails) {
        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginFileDetails.file(), pluginFileDetails.isBundledPlugin());

        validateIfExternalPluginRemovingBundledPlugin(bundleDescriptor);
        validateIfSamePluginUpdated(bundleDescriptor);
        validatePluginCompatibilityWithCurrentOS(bundleDescriptor);
        validatePluginCompatibilityWithGoCD(bundleDescriptor);
        removePlugin(bundleDescriptor);
        addPlugin(pluginFileDetails, bundleDescriptor);
    }

    @Override
    public void pluginJarRemoved(PluginFileDetails pluginFileDetails) {
        GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(null, pluginFileDetails.file().getName());
        if (existingDescriptor == null) {
            return;
        }
        boolean externalPlugin = !pluginFileDetails.isBundledPlugin();
        boolean bundledPlugin = existingDescriptor.isBundledPlugin();
        boolean externalPluginWithSameIdAsBundledPlugin = bundledPlugin && externalPlugin;
        if (externalPluginWithSameIdAsBundledPlugin) {
            LOGGER.info("External Plugin file '{}' having same name as bundled plugin file has been removed. Refusing to unload bundled plugin with id: '{}'", pluginFileDetails.file(), existingDescriptor.id());
            return;
        }
        removePlugin(existingDescriptor.bundleDescriptor());
    }

    private void addPlugin(PluginFileDetails pluginFileDetails, GoPluginBundleDescriptor bundleDescriptor) {
        explodePluginJarToBundleDir(pluginFileDetails.file(), bundleDescriptor.bundleLocation());
        installActivatorJarToBundleDir(bundleDescriptor.bundleLocation());
        registry.loadPlugin(bundleDescriptor);
        refreshBundle(bundleDescriptor);
    }

    private void removePlugin(GoPluginBundleDescriptor descriptor) {
        final GoPluginBundleDescriptor descriptorOfRemovedPlugin = registry.unloadPlugin(descriptor);
        pluginLoader.unloadPlugin(descriptorOfRemovedPlugin);
        boolean bundleLocationHasBeenDeleted = FileUtils.deleteQuietly(descriptorOfRemovedPlugin.bundleLocation());
        if (!bundleLocationHasBeenDeleted) {
            throw new RuntimeException(String.format("Failed to remove bundle jar %s from bundle location %s", descriptorOfRemovedPlugin.fileName(), descriptorOfRemovedPlugin.bundleLocation()));
        }
    }

    private void validateIfExternalPluginRemovingBundledPlugin(GoPluginBundleDescriptor newBundleDescriptor) {
        for (GoPluginDescriptor newPluginDescriptor : newBundleDescriptor.descriptors()) {
            final GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(newPluginDescriptor.id(), newBundleDescriptor.fileName());
            if (existingDescriptor != null && existingDescriptor.isBundledPlugin() && !newBundleDescriptor.isBundledPlugin()) {
                throw new RuntimeException(String.format("Found bundled plugin with ID: [%s], external plugin could not be loaded", existingDescriptor.id()));
            }
        }
    }

    private void validateIfSamePluginUpdated(GoPluginBundleDescriptor newBundleDescriptor) {
        final GoPluginDescriptor existingDescriptor = registry.getPluginByIdOrFileName(newBundleDescriptor.id(), newBundleDescriptor.fileName());
        if (existingDescriptor != null && !existingDescriptor.fileName().equals(newBundleDescriptor.fileName())) {
            throw new RuntimeException("Found another plugin with ID: " + existingDescriptor.id());
        }
    }

    private void validatePluginCompatibilityWithCurrentOS(GoPluginBundleDescriptor bundleDescriptor) {
        String currentOS = systemEnvironment.getOperatingSystemFamilyName();

        for (GoPluginDescriptor pluginDescriptor : bundleDescriptor.descriptors()) {
            if (!pluginDescriptor.isCurrentOSValidForThisPlugin(currentOS)) {
                markAllPluginsInBundleAsInvalid(bundleDescriptor, "Incompatible with current operating system '%s'. Valid operating systems are: %s.",
                        currentOS, pluginDescriptor.about().targetOperatingSystems());
            }
        }
    }

    private void validatePluginCompatibilityWithGoCD(GoPluginBundleDescriptor descriptor) {
        try {
            if (!descriptor.isCurrentGocdVersionValidForThisPlugin()) {
                List<String> messages = Arrays.asList(String.format("Plugin with ID (%s) is not valid: Incompatible with GoCD version '%s'. Compatible version is: %s.",
                        descriptor.id(), CurrentGoCDVersion.getInstance().goVersion(), descriptor.about().targetGoVersion()));
                descriptor.markAsInvalid(messages, null);
            }
        } catch (IllegalArgumentException e) {
            List<String> messages = Arrays.asList(String.format("Plugin with ID (%s) is not valid: Incorrect target gocd version(%s) specified.", descriptor.id(), descriptor.about().targetGoVersion()));
            descriptor.markAsInvalid(messages, null);
        }
    }

    private void refreshBundle(GoPluginBundleDescriptor descriptor) {
        if (!descriptor.isInvalid()) {
            osgiManifestGenerator.updateManifestOf(descriptor);
            pluginLoader.loadPlugin(descriptor);
        }
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

    private void markAllPluginsInBundleAsInvalid(GoPluginBundleDescriptor bundleDescriptor, String format, Object... values) {
        String prefix = String.format(bundleDescriptor.descriptors().size() > 1 ? "Plugins with IDs (%s) are not valid: " : "Plugin with ID (%s) is not valid: ", bundleDescriptor.pluginIDs());
        bundleDescriptor.markAsInvalid(singletonList(String.format(prefix + format, values)), null);
    }
}
