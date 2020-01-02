/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv6.plugininfos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv6.plugininfos.representers.extensions.ExtensionRepresenter;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus;
import com.thoughtworks.go.spark.Routes;

public class PluginInfoRepresenter {

    public static void toJSON(OutputWriter outputWriter, CombinedPluginInfo pluginInfo) {
        GoPluginDescriptor descriptor = (GoPluginDescriptor) pluginInfo.getDescriptor();
        outputWriter
                .addLinks(linksWriter -> {
                    linksWriter
                            .addLink("self", Routes.PluginInfoAPI.id(descriptor.id()))
                            .addAbsoluteLink("doc", Routes.PluginInfoAPI.DOC)
                            .addLink("find", Routes.PluginInfoAPI.find());
                    if (pluginInfo.getImage() != null) {
                        linksWriter.addLink("image", Routes.PluginImages.pluginImage(descriptor.id(), pluginInfo.getImage().getHash()));
                    }
                })
                .add("id", descriptor.id())
                .addChild("status", statusWriter -> {
                    PluginStatus.State state = descriptor.getStatus().getState();
                    statusWriter.add("state", state.toString().toLowerCase());
                    if (state == PluginStatus.State.INVALID) {
                        statusWriter.addChildList("messages", descriptor.getStatus().getMessages());
                    }
                })
                .add("plugin_file_location", descriptor.pluginFileLocation())
                .add("bundled_plugin", descriptor.isBundledPlugin());

        if (descriptor.about() != null) {
            outputWriter.addChild("about", listWriter -> listWriter
                    .add("name", descriptor.about().name())
                    .add("version", descriptor.about().version())
                    .add("target_go_version", descriptor.about().targetGoVersion())
                    .add("description", descriptor.about().description())
                    .addChildList("target_operating_systems", descriptor.about().targetOperatingSystems())
                    .addChild("vendor", vendorWriter -> vendorWriter
                            .add("name", descriptor.about().vendor().name())
                            .add("url", descriptor.about().vendor().url()))
            );
        }

        outputWriter.addChildList("extensions", extensionsWriter -> pluginInfo.getExtensionInfos()
                .forEach(extension -> {
                    ExtensionRepresenter extensionRepresenter = ExtensionRepresenterResolver.resolveRepresenterFor(extension);
                    if (extensionRepresenter != null) {
                        extensionsWriter.addChild(extensionWriter -> extensionRepresenter.toJSON(extensionWriter, extension));
                    }
                }));
    }
}
