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

package com.thoughtworks.go.util;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.Collections;
import java.util.List;

public class NoPluginsInstalledPluginManager implements PluginManager {
    @Override
    public List<GoPluginDescriptor> plugins() {
        return Collections.emptyList();
    }

    @Override
    public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
        return null;
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler) {
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches) {
        throw new RuntimeException("Invalid plugin: " + pluginId);
    }

    @Override
    public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        throw new RuntimeException("Invalid plugin: " + pluginId);
    }

    @Override
    public <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
    }

    @Override
    public void startPluginInfrastructure() {
    }

    @Override
    public void stopPluginInfrastructure() {
    }

    @Override
    public void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {
    }

    @Override
    public GoPluginApiResponse submitTo(String pluginId, GoPluginApiRequest apiRequest) {
        throw new RuntimeException("Invalid plugin: " + pluginId);
    }

    @Override
    public List<GoPluginIdentifier> allPluginsOfType(String extension) {
        return null;
    }
}
