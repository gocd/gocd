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
 * Annotation that marks a type an extension implementation.
 * <p>
 * Intended for use by plugin developers to mark an implementation
 * of the Go extension API.
 * When a type is marked with annotation the type will be loaded
 * by the plugin framework and will receive the lifecycle callbacks
 * of the Go API interfaces it implements.
 * </p>
 * Please refer to the Go Plugin documentation for more details on how to use
 * this annotation.
 *
 * @see <a href="https://developer.gocd.org/current/writing_go_plugins/go_plugins_basics.html" target="_blank">Go Plugin Documentation</a>
 *
 * @author Go Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extension {
}
