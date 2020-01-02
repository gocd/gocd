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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

public abstract class ErrorCollectingHandler implements GoConfigGraphWalker.Handler {
    private final List<ConfigErrors> allErrors;

    public ErrorCollectingHandler(List<ConfigErrors> allErrors) {
        this.allErrors = allErrors;
    }

    @Override
    public void handle(Validatable validatable, ValidationContext context) {
        handleValidation(validatable, context);
        ConfigErrors configErrors = validatable.errors();

        if (!configErrors.isEmpty()) {
            allErrors.add(configErrors);
        }
    }

    public abstract void handleValidation(Validatable validatable, ValidationContext context);
}
