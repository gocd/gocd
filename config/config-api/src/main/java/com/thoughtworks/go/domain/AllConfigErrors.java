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

package com.thoughtworks.go.domain;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AllConfigErrors extends ArrayList<ConfigErrors> {
    public AllConfigErrors(List<ConfigErrors> errors) {
        addAll(errors);
    }

    public AllConfigErrors() {
    }

    public String asString() {
        return StringUtils.join(this.stream().map(new Function<ConfigErrors, String>() {
            @Override
            public String apply(ConfigErrors errors) {
                return errors.asString();
            }
        }).collect(Collectors.toList()), ", ");
    }
}
