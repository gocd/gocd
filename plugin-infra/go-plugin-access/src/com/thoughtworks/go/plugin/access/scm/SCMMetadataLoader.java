/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.apache.log4j.Logger.getLogger;

@Component
public class SCMMetadataLoader implements PluginChangeListener {
    private static final Logger LOGGER = getLogger(SCMMetadataLoader.class);

    SCMExtension scmExtension;
    private SCMMetadataStore scmMetadataStore = SCMMetadataStore.getInstance();

    @Autowired
    public SCMMetadataLoader(SCMExtension scmExtension, PluginManager pluginManager) {
        this.scmExtension = scmExtension;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
    }

    void fetchSCMMetaData(GoPluginDescriptor pluginDescriptor) {
        try {
            SCMPropertyConfiguration scmConfiguration = scmExtension.getSCMConfiguration(pluginDescriptor.id());
            if (scmConfiguration == null) {
                throw new RuntimeException(format("Plugin[%s] returned null SCM configuration", pluginDescriptor.id()));
            }
            SCMView scmView = scmExtension.getSCMView(pluginDescriptor.id());
            if (scmView == null) {
                throw new RuntimeException(format("Plugin[%s] returned null SCM view", pluginDescriptor.id()));
            }
            scmMetadataStore.addMetadataFor(pluginDescriptor.id(), new SCMConfigurations(scmConfiguration), scmView);
        } catch (GoPluginFrameworkException e) {
            LOGGER.error(format("Failed to fetch SCM metadata for plugin : %s", pluginDescriptor.id()), e);
        }
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (scmExtension.canHandlePlugin(pluginDescriptor.id())) {
            fetchSCMMetaData(pluginDescriptor);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (scmExtension.canHandlePlugin(pluginDescriptor.id())) {
            scmMetadataStore.removeMetadata(pluginDescriptor.id());
        }
    }
}
