/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.launcher.DownloadableFile;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class AgentPluginsInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Log LOG = LogFactory.getLog(AgentPluginsInitializer.class);
    private final DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    private PluginManager pluginManager;
    private ZipUtil zipUtil;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public AgentPluginsInitializer(PluginManager pluginManager, DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor,
                                   ZipUtil zipUtil, SystemEnvironment systemEnvironment) {
        this.pluginManager = pluginManager;
        this.defaultPluginJarLocationMonitor = defaultPluginJarLocationMonitor;
        this.zipUtil = zipUtil;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            File agentPluginsZip = new File(DownloadableFile.AGENT_PLUGINS.getLocalFileName());
            File pluginsFolder = new File(systemEnvironment.get(SystemEnvironment.AGENT_PLUGINS_PATH));

            if (pluginsFolder.exists()) {
                FileUtils.forceDelete(pluginsFolder);
            }
            zipUtil.unzip(agentPluginsZip, pluginsFolder);
            defaultPluginJarLocationMonitor.initialize();
            pluginManager.startInfrastructure();
        } catch (IOException e) {
            LOG.warn("could not extract plugin zip", e);
        } catch (RuntimeException e) {
            LOG.warn("error while initializing agent plugins", e);
        }
    }
}
