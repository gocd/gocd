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

package com.thoughtworks.go.plugin.infra.listeners;

import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarChangeListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginsZipUpdater implements PluginJarChangeListener {

    private PluginsZip pluginsZip;

    @Autowired
    public PluginsZipUpdater(PluginsZip pluginsZip) {
        this.pluginsZip = pluginsZip;
    }

    @Override
    public void pluginJarAdded(PluginFileDetails pluginFileDetails) {
        pluginsZip.create();
    }

    @Override
    public void pluginJarUpdated(PluginFileDetails pluginFileDetails) {
        pluginsZip.create();
    }

    @Override
    public void pluginJarRemoved(PluginFileDetails pluginFileDetails) {
        pluginsZip.create();
    }
}
