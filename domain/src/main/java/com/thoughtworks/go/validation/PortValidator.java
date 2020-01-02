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
package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;

public class PortValidator extends Validator<String> {
    public static final String ERROR_MESSAGE = "Port must be a number less than 65535.";

    public PortValidator() {
        super(ERROR_MESSAGE);
    }

    @Override
    public ValidationBean validate(String port) {
        try {
            if (Integer.valueOf(port) <= 0 || Integer.valueOf(port) > 65535) {
                return ValidationBean.notValid(ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            return ValidationBean.notValid(ERROR_MESSAGE);
        }
        return ValidationBean.valid();
    }
}
