/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.List;

public interface PluginManager {
    List<GoPluginDescriptor> plugins();

    GoPluginDescriptor getPluginDescriptorFor(String pluginId);

    <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches);

    <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> exceptionHandler);

    <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> actionToDoOnTheRegisteredServiceWhichMatches);

    <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action);

    <T> void doOnIfHasReference(Class<T> serviceReferenceClass, String pluginId, Action<T> action);

    void startInfrastructure(boolean shouldPoll);

    void stopInfrastructure();

    void addPluginChangeListener(PluginChangeListener pluginChangeListener, Class<?>... serviceReferenceClass);

    GoPluginApiResponse submitTo(String pluginId, GoPluginApiRequest apiRequest);

    boolean hasReferenceFor(Class serviceReferenceClass, String pluginId);

    boolean isPluginOfType(String extension, String pluginId);

    String resolveExtensionVersion(String pluginId, List<String> goSupportedExtensionVersions);
}
