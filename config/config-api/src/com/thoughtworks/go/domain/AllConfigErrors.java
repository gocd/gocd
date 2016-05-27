/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.ListUtil;

import java.util.ArrayList;
import java.util.List;

public class AllConfigErrors extends ArrayList<ConfigErrors> {
    public AllConfigErrors(List<ConfigErrors> errors) {
        addAll(errors);
    }

    public AllConfigErrors() {
    }

    public String asString() {
        return ListUtil.join(ListUtil.map(this, new ListUtil.Transformer<ConfigErrors, String>() {
            @Override
            public String transform(ConfigErrors errors) {
                return errors.asString();
            }
        }));
    }
}
