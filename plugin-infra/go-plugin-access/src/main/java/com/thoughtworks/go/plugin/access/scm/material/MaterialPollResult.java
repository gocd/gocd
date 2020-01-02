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
package com.thoughtworks.go.plugin.access.scm.material;

import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MaterialPollResult {
    private Map<String, String> materialData;
    private List<SCMRevision> revisions;

    public MaterialPollResult() {
    }

    public MaterialPollResult(Map<String, String> data, SCMRevision revision) {
        this(data, revision == null ? null : Arrays.asList(revision));
    }

    public MaterialPollResult(Map<String, String> materialData, List<SCMRevision> revisions) {
        this.materialData = materialData;
        this.revisions = revisions;
    }

    public Map<String, String> getMaterialData() {
        return materialData;
    }

    public SCMRevision getLatestRevision() {
        return revisions == null ? null : revisions.get(0);
    }

    public List<SCMRevision> getRevisions() {
        return revisions;
    }
}
