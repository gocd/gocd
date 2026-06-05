/*
 * Copyright Thoughtworks, Inc.
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

import java.util.Objects;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;

public class StageConfigIdentifier {
    private final CaseInsensitiveString pipelineName;
    private final CaseInsensitiveString stageName;

    public StageConfigIdentifier(String pipelineName, String stageName) {
        this(cis(pipelineName), cis(stageName));
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
        return Objects.equals(pipelineName, that.pipelineName) &&
            Objects.equals(stageName, that.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineName, stageName);
    }

    @Override
    public String toString() {
        return "StageConfigIdentifier[" + pipelineName + ":" + stageName + "]";
    }
}
