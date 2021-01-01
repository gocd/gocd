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
package com.thoughtworks.go.apiv7.plugininfos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;

public class PluginInfosRepresenter {

    public static void toJSON(OutputWriter writer, Collection<CombinedPluginInfo> pluginInfos) {
        writer.addLinks(
                outputLinkWriter -> outputLinkWriter
                        .addLink("self", Routes.PluginInfoAPI.BASE)
                        .addAbsoluteLink("doc", Routes.PluginInfoAPI.DOC)
                        .addLink("find", Routes.PluginInfoAPI.find()))
                .addChild("_embedded",
                        embeddedWriter -> embeddedWriter.addChildList("plugin_info",
                                pluginInfosWriter -> pluginInfos.forEach(
                                        store -> pluginInfosWriter.addChild(
                                                pluginInfoWriter -> PluginInfoRepresenter.toJSON(pluginInfoWriter, store)))));
    }
}
