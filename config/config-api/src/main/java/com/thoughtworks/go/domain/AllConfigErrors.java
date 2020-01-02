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
package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllConfigErrors extends ArrayList<ConfigErrors> {
    public AllConfigErrors(List<ConfigErrors> errors) {
        addAll(errors);
    }

    public AllConfigErrors() {
    }

    public String asString() {
        return String.join(", ", this.stream().map(ConfigErrors::asString).collect(Collectors.toList()));
    }
}
