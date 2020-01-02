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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;

public class StageConfigIdentifier {
private final CaseInsensitiveString pipelineName;
    private final CaseInsensitiveString stageName;

    public StageConfigIdentifier(String pipelineName, String stageName) {
        this(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    private StageConfigIdentifier(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    public String getPipelineName() {
        return CaseInsensitiveString.str(pipelineName);
    }

    public String getStageName() {
        return CaseInsensitiveString.str(stageName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageConfigIdentifier that = (StageConfigIdentifier) o;

        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineName != null ? pipelineName.hashCode() : 0;
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        return result;
    }
}
