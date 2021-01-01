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
package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.CaseInsensitiveString;

public class StageNotFoundException extends RecordNotFoundException {
    private final String pipelineName;
    private final String stageName;

    public StageNotFoundException(String pipelineName, String stageName) {
        super(String.format("Stage '%s' not found in pipeline '%s'", stageName, pipelineName));

        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    public StageNotFoundException(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        this(CaseInsensitiveString.str(pipelineName), CaseInsensitiveString.str(stageName));
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public static void bombIfNull(Object obj, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        if (obj == null) {
            throw new StageNotFoundException(pipelineName, stageName);
        }
    }
}
