/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GoConfigInvalidException extends RuntimeException {
    private final CruiseConfig cruiseConfig;
    private final Set<String> allErrors;

    public GoConfigInvalidException(CruiseConfig cruiseConfig, String error) {
        super(error);
        allErrors = Set.of(error);
        this.cruiseConfig = cruiseConfig;
    }

    public GoConfigInvalidException(CruiseConfig cruiseConfig, List<ConfigErrors> errors) {
        super(firstError(errors));
        allErrors = extractErrors(errors);
        this.cruiseConfig = cruiseConfig;
    }

    protected GoConfigInvalidException(CruiseConfig cruiseConfig, List<ConfigErrors> errors, Throwable e) {
        super(firstError(errors), e);
        this.cruiseConfig = cruiseConfig;
        this.allErrors = extractErrors(errors);
    }

    private static Set<String> extractErrors(List<ConfigErrors> errors) {
        Set<String> allErrors = new LinkedHashSet<>();
        for (ConfigErrors er : errors) {
            allErrors.addAll(er.getAll());
        }
        return allErrors;
    }

    private static String firstError(List<ConfigErrors> errors) {
        if (!errors.isEmpty()) {
            return errors.get(0).asString();
        }
        return null;
    }

    public CruiseConfig getCruiseConfig() {
        return cruiseConfig;
    }

    public String getAllErrorMessages() {
        return new AllConfigErrors(cruiseConfig.getAllErrors()).asString();
    }

    public List<String> getAllErrors() {
        return new ArrayList<>(allErrors);
    }
}
