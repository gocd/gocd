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

package com.thoughtworks.go.server.service.plugins;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataLoader;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.access.common.PluginMetadataChangeListener;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Service
public class AnalyticsPluginAssetsService implements ServletContextAware, PluginMetadataChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsPluginAssetsService.class);
    public static final String HASH_ALGORITHM = "SHA-256";
    private AnalyticsExtension analyticsExtension;
    private ServletContext servletContext;
    private ZipUtil zipUtil;
    private Map<String, String> pluginAssetPaths;
    private AnalyticsMetadataStore metadataStore;

    @Autowired
    public AnalyticsPluginAssetsService(AnalyticsExtension analyticsExtension, AnalyticsMetadataLoader metadataLoader) {
        this.zipUtil = new ZipUtil();
        this.analyticsExtension = analyticsExtension;
        this.pluginAssetPaths = new HashMap<>();
        metadataLoader.registerListeners(this);
        metadataStore = AnalyticsMetadataStore.instance();
    }

    public String assetPathFor(String pluginId) {
        return pluginAssetPaths.get(pluginId);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void onPluginMetadataCreate(String pluginId) {
        if (this.analyticsExtension.canHandlePlugin(pluginId)) {
            deleteExistingAssets(pluginId);
            cacheStaticAssets(pluginId);
            metadataStore.updateAssetsPath(pluginId, assetPathFor(pluginId));
        }
    }

    @Override
    public void onPluginMetadataRemove(String pluginId) {
        if (this.analyticsExtension.canHandlePlugin(pluginId)) {
            deleteExistingAssets(pluginId);
        }
    }

    private String currentAssetPath(String pluginId, String hash) {
        return Paths.get(pluginStaticAssetsRootDir(pluginId), hash).toString();
    }

    private String pluginStaticAssetsRootDir(String pluginId) {
        return Paths.get(servletContext.getRealPath(servletContext.getInitParameter("rails.root")), "public", "assets", "plugins", pluginId).toString();
    }

    private void cacheStaticAssets(String pluginId) {
        LOGGER.info("Caching static assets for plugin: {}", pluginId);
        String data = this.analyticsExtension.getStaticAssets(pluginId);

        if (StringUtils.isBlank(data)) {
            LOGGER.info("No static assets found for plugin: {}", pluginId);
            return;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(data.getBytes());
            ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(payload));

            String pluginAssetsRoot = currentAssetPath(pluginId, calculateHash(payload));
            zipUtil.unzip(zipInputStream, new File(pluginAssetsRoot));

            pluginAssetPaths.put(pluginId, pluginAssetsRoot);
        } catch (Exception e) {
            LOGGER.error("Failed to extract static assets from plugin: {}", pluginId, e);
            ExceptionUtils.bomb(e);
        }
    }

    private String calculateHash(byte[] data) {
        return sha2Digest(data);
    }

    private String sha2Digest(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            return DatatypeConverter.printHexBinary(md.digest(bytes));
        } catch (Exception e) {
            LOGGER.error("Error generating {} hash", HASH_ALGORITHM, e);
            ExceptionUtils.bomb(e);
        }

        return null;
    }

    private void deleteExistingAssets(String pluginId) {
        LOGGER.info("Deleting cached static assets for plugin: {}", pluginId);
        try {
            FileUtils.deleteDirectory(new File(pluginStaticAssetsRootDir(pluginId)));
            if (pluginAssetPaths.containsKey(pluginId)) {
                pluginAssetPaths.remove(pluginId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete cached static assets for plugin: {}", pluginId, e);
            ExceptionUtils.bomb(e);
        }
    }
}
