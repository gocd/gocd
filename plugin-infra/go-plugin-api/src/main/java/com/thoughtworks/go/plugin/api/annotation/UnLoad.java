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
package com.thoughtworks.go.plugin.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method to be called when a plugin is unloaded.
 * <p>
 * Intended for use by plugin developers to mark a method of an
 * extension implementation to be called during plugin unload time.
 * </p>
 * The semantics of the method marked with this annotation is as follows.
 * <ul>
 *     <li>There should atmost one method in the implementation that should have this annotation.</li>
 *     <li>The method should be public, non-static.</li>
 *     <li>The method should take only one parameter- {@link com.thoughtworks.go.plugin.api.info.PluginContext}. Return values will be ignored.</li>
 *     <li>This annotation will not be inherited along with the method.</li>
 * </ul>
 *
 * The framework performs the validation of the semantics mentioned above during the plugin load time - before executing the
 * method marked with {@link Load} annotation. But the callback to this method is only done during the plugin unload time.
 * <i>In summary, semantics checks are done at the plugin load and invocation done during the plugin unload.</i>
 *
 * Please refer to the Go Plugin documentation for more details.
 *
 * @see <a href="https://developer.gocd.org/current/writing_go_plugins/go_plugins_basics.html" target="_blank">Go Plugin Documentation</a>
 * @see com.thoughtworks.go.plugin.api.info.PluginContext
 * @see Load
 *
 * @author Go Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnLoad {
}
