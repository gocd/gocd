/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.rules;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public enum DirectiveType {
    ALLOW("allow"), DENY("deny");

    private final String type;

    DirectiveType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public static Optional<DirectiveType> fromString(String directive) {
        if (StringUtils.isBlank(directive)) {
            return Optional.empty();
        }

        switch (directive) {
            case "allow":
                return Optional.of(ALLOW);
            case "deny":
                return Optional.of(DENY);
            default:
                return Optional.empty();
        }
    }
}
