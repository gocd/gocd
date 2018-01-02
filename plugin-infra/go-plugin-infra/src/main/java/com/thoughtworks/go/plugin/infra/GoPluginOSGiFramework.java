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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.osgi.framework.Bundle;

public interface GoPluginOSGiFramework {
    void start();

    void stop();

    Bundle loadPlugin(GoPluginDescriptor pluginDescriptor);

    void unloadPlugin(GoPluginDescriptor pluginDescriptor);

    void addPluginChangeListener(PluginChangeListener pluginChangeListener);

    <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches);

    <T> void doOnAllWithExceptionHandling(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> handler);

    <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> action);

    <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action);

    <T> void doOnWithExceptionHandling(Class<T> serviceReferenceClass, String pluginId, Action<T> action, ExceptionHandler<T> handler);

    <T> void doOnAllForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action);

    <T> void doOnAllWithExceptionHandlingForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action, ExceptionHandler<T> handler);

    <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId);


}
