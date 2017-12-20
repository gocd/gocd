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
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.ZipInputStream;

@Component
public class PluginAssetsLoader implements ServletContextAware, PluginChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAssetsLoader.class);
    private AnalyticsExtension analyticsExtension;
    private ServletContext servletContext;
    private ZipUtil zipUtil;

    @Autowired
    public PluginAssetsLoader(AnalyticsExtension analyticsExtension) {
        this.zipUtil = new ZipUtil();
        this.analyticsExtension = analyticsExtension;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (this.analyticsExtension.canHandlePlugin(pluginDescriptor.id())) {
            deleteExistingAssets(pluginDescriptor.id());
            cacheStaticAssets(pluginDescriptor.id());
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (this.analyticsExtension.canHandlePlugin(pluginDescriptor.id())) {
            deleteExistingAssets(pluginDescriptor.id());
        }
    }

    private String pluginStaticAssetsDirPath(String pluginId) {
        return Paths.get(servletContext.getRealPath(servletContext.getInitParameter("rails.root")), "public", "assets", "plugins", pluginId).toString();
    }

    private void cacheStaticAssets(String pluginId) {
        LOGGER.info("Caching static assets for plugin: {}", pluginId);
        String data = this.analyticsExtension.getStaticAssets(pluginId);

        if (StringUtil.isBlank(data)) {
            LOGGER.info("No static assets found for plugin: {}", pluginId);
            return;
        }

        try {
            ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data.getBytes())));

            zipUtil.unzip(zipInputStream, new File(pluginStaticAssetsDirPath(pluginId)));
        } catch (IOException e) {
            LOGGER.error("Failed to extract static assets from plugin: {}", pluginId, e);
        }
    }

    private void deleteExistingAssets(String pluginId) {
        LOGGER.info("Deleting cached static assets for plugin: {}", pluginId);
        try {
            FileUtil.deleteDirectoryNoisily(new File(pluginStaticAssetsDirPath(pluginId)));
        } catch (IOException e) {
            LOGGER.error("Failed to delete cached static assets for plugin: {}", pluginId, e);
        }
    }
}
