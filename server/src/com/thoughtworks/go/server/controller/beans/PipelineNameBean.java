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

package com.thoughtworks.go.server.controller.beans;

import java.util.regex.Pattern;

import com.thoughtworks.go.domain.materials.ValidationBean;
import static com.thoughtworks.go.validation.PipelineGroupValidator.ERRORR_MESSAGE;
import org.apache.commons.lang.StringUtils;

public class PipelineNameBean {
    private final String pipelineName;
    private boolean hasPipeline;

    public static final String NAME_PATTERN = "[a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*";
    private static final Pattern VALID_PROJECTNAME = Pattern.compile(NAME_PATTERN);


    public PipelineNameBean(String pipelineName, boolean hasPipeline) {
        this.pipelineName = pipelineName;
        this.hasPipeline = hasPipeline;
    }

    private boolean validateProjectName() {
        return pipelineName.matches(NAME_PATTERN);

    }

    public ValidationBean validate() {
        if (StringUtils.isEmpty(pipelineName)) {
            return ValidationBean.notValid("Pipeline name is required");
        } else if (StringUtils.isBlank(pipelineName) || !validateProjectName()) {
            return ValidationBean.notValid(ERRORR_MESSAGE);
        } else if (hasPipeline) {
            return ValidationBean.notValid("Pipeline name already exists, please choose another one.");
        } else {
            return ValidationBean.valid();
        }
    }
}
