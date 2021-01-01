/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.configrepooperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreflightResult {
    private Set<String> errors = new HashSet<>();
    private boolean valid = false;

    public PreflightResult() {
    }

    public PreflightResult update(List<String> errors, boolean valid) {
        this.errors.addAll(errors);
        this.valid = valid;
        return this;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setErrors(List<String> errors) {
        this.errors.clear();
        this.errors.addAll(errors);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
