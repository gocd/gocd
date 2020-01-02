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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Map;

public interface GoPluginOSGiFramework {
    void start();

    void stop();

    Bundle loadPlugin(GoPluginBundleDescriptor pluginBundleDescriptor);

    void unloadPlugin(GoPluginBundleDescriptor pluginBundleDescriptor);

    <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, String extensionType, ActionWithReturn<T, R> action);

    <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId, String extensionType);

    Map<String, List<String>> getExtensionsInfoFromThePlugin(String pluginId);
}
