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

package com.thoughtworks.go.plugins;

import java.util.List;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.ExceptionHandler;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

public abstract class FakePluginManager implements PluginManager {
    @Override
    public List<GoPluginDescriptor> plugins() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public GoPluginDescriptor getPluginDescriptorFor(String pluginId) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void startInfrastructure(boolean shouldPoll) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void registerPluginsFolderChangeListener() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void stopInfrastructure() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public GoPluginApiResponse submitTo(String pluginId, GoPluginApiRequest apiRequest) {
        return null;
    }

    @Override
    public List<GoPluginIdentifier> allPluginsOfType(String extension) {
        return null;
    }

    @Override
    public String resolveExtensionVersion(String pluginId, List<String> goSupportedExtensionVersions) {
        return null;
    }
}
