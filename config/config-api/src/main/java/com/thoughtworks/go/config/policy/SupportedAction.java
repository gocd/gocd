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

package com.thoughtworks.go.config.policy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public enum SupportedAction {
    VIEW("view"),
    EDIT("edit"),
    ADMINISTER("administer"),
    UNKNOWN("unknown");

    private final String action;

    SupportedAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static List<String> unmodifiableListOf(SupportedAction... supportedActions) {
        return unmodifiableList(Arrays.stream(supportedActions)
                .map(SupportedAction::getAction)
                .collect(Collectors.toList()));
    }
}
