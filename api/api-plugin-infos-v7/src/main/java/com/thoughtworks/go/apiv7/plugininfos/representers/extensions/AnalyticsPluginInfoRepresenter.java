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
package com.thoughtworks.go.apiv7.plugininfos.representers.extensions;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class AnalyticsPluginInfoRepresenter extends ExtensionRepresenter {
    @Override
    public void toJSON(OutputWriter extensionWriter, PluginInfo extension) {
        super.toJSON(extensionWriter, extension);

        AnalyticsPluginInfo analyticsPluginInfo = (AnalyticsPluginInfo) extension;

        extensionWriter.addChild("capabilities", capabilitiesWriter ->
                capabilitiesWriter.addChildList("supported_analytics", supportedAnalyticsWriter ->
                        analyticsPluginInfo.getCapabilities().getSupportedAnalytics().forEach(analytics ->
                                supportedAnalyticsWriter.addChild(supportedAnalyticWriter -> supportedAnalyticWriter.add("type", analytics.getType())
                                        .add("id", analytics.getId())
                                        .add("title", analytics.getTitle())))));
    }
}
