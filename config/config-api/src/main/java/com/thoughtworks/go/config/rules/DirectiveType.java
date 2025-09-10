/*
 * Copyright Thoughtworks, Inc.
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
        if (directive == null) {
            return Optional.empty();
        }

        return switch (directive) {
            case "allow" -> Optional.of(ALLOW);
            case "deny" -> Optional.of(DENY);
            default -> Optional.empty();
        };
    }
}
