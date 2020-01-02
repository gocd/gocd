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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.Validatable;

import java.util.List;

public interface PolicyAware {
    List<String> allowedActions();

    List<String> allowedTypes();

    Policy getPolicy();

    default boolean hasPermissionsFor(SupportedAction action, Class<? extends Validatable> entityType, String resource) {
        Policy policy = getPolicy();
        if (policy == null || policy.isEmpty()) {
            return false;
        }

        return policy.stream()
                .map(directive -> directive.apply(action.getAction(), entityType, resource))
                .filter(result -> result != Result.SKIP)
                .map(result -> result == Result.ALLOW)
                .findFirst()
                .orElse(false);
    }

    default boolean hasExplicitDenyPermissionsFor(SupportedAction action, Class<? extends Validatable> entityType, String resource) {
        Policy policy = getPolicy();
        if (policy == null || policy.isEmpty()) {
            return false;
        }

        return policy.stream()
                .map(directive -> directive.apply(action.getAction(), entityType, resource))
                .filter(result -> result != Result.SKIP)
                .map(result -> result == Result.DENY)
                .findFirst()
                .orElse(false);
    }
}
