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

package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;
import org.apache.commons.lang.StringUtils;

public class LengthValidator extends Validator<String> {
    private final int length;

    public LengthValidator(int length) {
        super("Only " + length + " charactors are allowed");
        this.length = length;
    }

    public ValidationBean validate(String value) {
        if (StringUtils.isEmpty(value)) {
            return ValidationBean.valid();
        }
        if (value.length() <= length) {
            return ValidationBean.valid();
        } else {
            return ValidationBean.notValid(errorMessage);
        }
    }
}
