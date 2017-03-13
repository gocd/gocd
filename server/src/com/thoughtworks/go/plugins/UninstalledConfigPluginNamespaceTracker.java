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

package com.thoughtworks.go.plugins;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.plugins.GoPluginManifest;
import org.jdom2.Namespace;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.springframework.stereotype.Component;

/**
 * @understands keeping track of uninstalled plugins
 */
@Component
public class UninstalledConfigPluginNamespaceTracker implements BundleListener {
    private List<Namespace> namespacesOfUninstalledPlugins;

    public UninstalledConfigPluginNamespaceTracker() {
        synchronized (this){
            namespacesOfUninstalledPlugins = new ArrayList<>();
        }
    }

    public List<Namespace> getNamespacesOfUninstalledPlugins() {
        return namespacesOfUninstalledPlugins;
    }

    public void bundleChanged(BundleEvent event) {
        if((event.getType() & BundleEvent.UNINSTALLED) != 0){
            GoPluginManifest manifest = new GoPluginManifest(event.getBundle());
            namespacesOfUninstalledPlugins.add(manifest.getPluginNamespace());
        }
    }
}
