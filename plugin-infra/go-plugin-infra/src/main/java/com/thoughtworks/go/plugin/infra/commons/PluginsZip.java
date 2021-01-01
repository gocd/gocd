/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.commons;

import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class PluginsZip implements PluginChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(PluginsZip.class);
    private static final Function<GoPluginDescriptor, String> PLUGIN_ID_COMPARATOR = GoPluginDescriptor::id;
    private static final Function<GoPluginDescriptor, Boolean> IS_BUNDLED_PLUGIN_COMPARATOR = GoPluginDescriptor::isBundledPlugin;
    private final Comparator<GoPluginDescriptor> PLUGIN_COMPARATOR = Comparator.comparing(IS_BUNDLED_PLUGIN_COMPARATOR).thenComparing(PLUGIN_ID_COMPARATOR);

    private final Predicate<GoPluginDescriptor> predicate;
    private String md5DigestOfPlugins;
    private List<GoPluginBundleDescriptor> agentPlugins = new CopyOnWriteArrayList<>();
    private final File destZipFile;
    private final File bundledPlugins;
    private final File externalPlugins;
    private final PluginManager pluginManager;

    @Autowired
    public PluginsZip(SystemEnvironment systemEnvironment, PluginManager pluginManager) {
        destZipFile = new File(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        bundledPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH));
        externalPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH));
        this.pluginManager = pluginManager;
        this.pluginManager.addPluginChangeListener(this);
        predicate = goPluginDescriptor -> PluginsZip.this.pluginManager.isPluginOfType("task", goPluginDescriptor.id()) ||
                PluginsZip.this.pluginManager.isPluginOfType("scm", goPluginDescriptor.id()) ||
                PluginsZip.this.pluginManager.isPluginOfType("package-repository", goPluginDescriptor.id()) ||
                PluginsZip.this.pluginManager.isPluginOfType("artifact", goPluginDescriptor.id());
    }

    public void create() {
        checkFilesAccessibility(bundledPlugins, externalPlugins);
        reset();

        MessageDigest md5Digest = DigestUtils.getMd5Digest();
        try (ZipOutputStream zos = new ZipOutputStream(new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(destZipFile)), md5Digest))) {
            for (GoPluginBundleDescriptor agentPlugins : agentPlugins()) {
                String zipEntryPrefix = "external/";

                if (agentPlugins.isBundledPlugin()) {
                    zipEntryPrefix = "bundled/";
                }

                zos.putNextEntry(new ZipEntry(zipEntryPrefix + new File(agentPlugins.bundleJARFileLocation()).getName()));
                Files.copy(new File(agentPlugins.bundleJARFileLocation()).toPath(), zos);
                zos.closeEntry();
            }
        } catch (Exception e) {
            LOG.error("Could not create zip of plugins for agent to download.", e);
        }

        md5DigestOfPlugins = Hex.encodeHexString(md5Digest.digest());
    }

    private void reset() {
        md5DigestOfPlugins = null;
        agentPlugins.clear();
    }

    public String md5() {
        return md5DigestOfPlugins;
    }

    private List<GoPluginBundleDescriptor> agentPlugins() {
        if (agentPlugins.isEmpty()) {
            List<GoPluginBundleDescriptor> agentPlugins = pluginManager.plugins().stream().
                    filter(predicate).
                    sorted(PLUGIN_COMPARATOR).
                    map(GoPluginDescriptor::bundleDescriptor).
                    distinct().
                    collect(Collectors.toList());
            this.agentPlugins.addAll(agentPlugins);
        }

        return agentPlugins;
    }

    private void checkFilesAccessibility(File bundledPlugins, File externalPlugins) {
        boolean bundled = bundledPlugins.canRead();
        boolean external = externalPlugins.canRead();
        if (!bundled || !external) {
            String folder = bundled ? externalPlugins.getAbsolutePath() : bundledPlugins.getAbsolutePath();
            LOG.error("Could not read plugins. Please check access rights on files in folder: {}.", folder);
            throw new FileAccessRightsCheckException(String.format("Could not read plugins. Please make sure that the user running GoCD can access %s", folder));
        }
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        maybeUpdatePluginsZip(pluginDescriptor);
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (agentPlugins().contains(pluginDescriptor.bundleDescriptor())) {
            create();
        }
    }

    private void maybeUpdatePluginsZip(GoPluginDescriptor pluginDescriptor) {
        if (predicate.test(pluginDescriptor)) {
            create();
        }
    }
}
