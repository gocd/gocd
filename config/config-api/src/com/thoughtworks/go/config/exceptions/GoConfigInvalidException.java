/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.exceptions;

import java.util.List;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.ConfigErrors;

public class GoConfigInvalidException extends RuntimeException {
    private final CruiseConfig cruiseConfig;

    public GoConfigInvalidException(CruiseConfig cruiseConfig, List<ConfigErrors> allErrors) {
        super(allErrors.get(0).firstError());
        this.cruiseConfig = cruiseConfig;
    }
    protected GoConfigInvalidException(CruiseConfig cruiseConfig, String message,Throwable e) {
        super(message,e);
        this.cruiseConfig = cruiseConfig;
    }
    protected GoConfigInvalidException(CruiseConfig cruiseConfig, String message) {
        super(message);
        this.cruiseConfig = cruiseConfig;
    }

    public CruiseConfig getCruiseConfig() {
        return cruiseConfig;
    }
}
