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
package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

public class GoConfigInvalidMergeException extends GoConfigInvalidException {
    private List<PartialConfig> partialConfigs;

    public GoConfigInvalidMergeException(CruiseConfig cruiseConfig,
                                         List<PartialConfig> partialConfigs, List<ConfigErrors> allErrors) {
        super(cruiseConfig, allErrors);
        this.partialConfigs = partialConfigs;
    }

    public GoConfigInvalidMergeException(CruiseConfig cruiseConfig,
                                         List<PartialConfig> partialConfigs, List<ConfigErrors> allErrors, Throwable e) {
        super(cruiseConfig, allErrors, e);
        this.partialConfigs = partialConfigs;
    }

    public GoConfigInvalidMergeException(List<PartialConfig> partials, GoConfigInvalidException failed) {
        this(failed.getCruiseConfig(), partials, failed.getCruiseConfig().getAllErrors(), failed);
    }

    private static String allErrorsToString(List<String> allErrors) {
        if (allErrors == null || allErrors.size() == 0)
            return "Error list empty";// should never be

        StringBuilder b = new StringBuilder();
        b.append("Number of errors: ").append(allErrors.size()).append("+").append("\n");

        for (int i = 1; i <= allErrors.size(); i++) {
            b.append(i).append(". ").append(allErrors.get(i - 1)).append(";; \n");
        }

        return b.toString();
    }

    @Override
    public String getMessage() {
        return allErrorsToString(getAllErrors());
    }

    public List<PartialConfig> getPartialConfigs() {
        return partialConfigs;
    }
}
