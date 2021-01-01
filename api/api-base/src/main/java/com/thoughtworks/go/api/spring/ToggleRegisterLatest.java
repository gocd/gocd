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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.api.ApiVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level (i.e., spark controllers) annotation that allows API `latest-version` mime type route registration
 * to be togglable for a given {@link String} path prefix and {@link ApiVersion}.
 * <p>
 * Set the optional `as={@link String}` option to specify a toggle name to bind instead of generating
 * a toggle key from the path and version. This toggle key need not be unique among controllers; thus, one can
 * control a set of APIs (usually, related to the same feature) with a single toggle.
 * <p>
 * Controller classes can have multiple annotations of this type to specify complex matching patterns against
 * different path prefixes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ToggleRegisterLatest {
    /**
     * Specifies the path prefix to match routes against
     *
     * @return the {@link String} path prefix
     */
    String controllerPath();

    /**
     * Specifies the API version to match routes against
     *
     * @return the {@link ApiVersion}
     */
    ApiVersion apiVersion();

    /**
     * Set a toggle key to control this route. This need not be unique among controllers, thus allowing a single
     * toggle to control groups of APIs.
     *
     * @return the {@link String} toggle key name
     */
    String as() default "";

    /**
     * When true, also matches paths under {@link #controllerPath()}. Otherwise, perform exact path match.
     * This may be useful when the path you want to toggle is an ascendant of an existing API. This is likely
     * rare, but exists for that reason.
     * <p>
     * Defaults to true.
     *
     * @return whether or not to include sub paths when matching
     */
    boolean includeDescendants() default true;
}
