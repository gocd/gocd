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
package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;
import org.apache.commons.lang3.StringUtils;

public class PipelineGroupValidator extends Validator<String> {
    public static final String ERRORR_MESSAGE = "Invalid character. Please use a-z, A-Z, 0-9, underscore, "
            + "dot and hyphen.";
    public static final String NAME_PATTERN = "[a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*";

    public PipelineGroupValidator() {
        super(ERRORR_MESSAGE);
    }

    @Override
    public ValidationBean validate(String pipelineGroupName) {
        if (StringUtils.isEmpty(pipelineGroupName)) {
            return ValidationBean.valid();
        }
        boolean valid = pipelineGroupName.matches(NAME_PATTERN);
        return valid ? ValidationBean.valid() : ValidationBean.notValid(ERRORR_MESSAGE);
    }
}
