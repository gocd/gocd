/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.ListUtil;

import java.util.List;

public class GoConfigInvalidException extends RuntimeException {
    private final CruiseConfig cruiseConfig;

    public GoConfigInvalidException(CruiseConfig cruiseConfig, String error) {
        super(error);
        this.cruiseConfig = cruiseConfig;
    }

    public GoConfigInvalidException(CruiseConfig cruiseConfig, List<ConfigErrors> allErrors) {
        this(cruiseConfig, ListUtil.join(ListUtil.map(allErrors, new ListUtil.Transformer<String>() {
            @Override
            public String transform(Object obj) {
                ConfigErrors errors = (ConfigErrors) obj;
                return ListUtil.join(errors.getAll());
            }
        })));
    }

    public CruiseConfig getCruiseConfig() {
        return cruiseConfig;
    }
}
