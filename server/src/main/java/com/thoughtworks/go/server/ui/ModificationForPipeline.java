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
package com.thoughtworks.go.server.ui;

import java.util.Set;

import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.materials.Modification;

/**
 * @understands association of material revisions of a pipeline
 */
public class ModificationForPipeline {
    private final PipelineId pipelineId;
    private final Modification modification;
    private final String materialType;
    private final String materialFingerprint;

    public ModificationForPipeline(PipelineId pipelineId, Modification modification, String materialType, String materialFingerprint) {
        this.pipelineId = pipelineId;
        this.modification = modification;
        this.materialType = materialType;
        this.materialFingerprint = materialFingerprint;
    }

    public PipelineId getPipelineId() {
        return pipelineId;
    }

    public Set<String> getCardNumbersFromComments() {
        return modification.getCardNumbersFromComment();
    }

    public Author getAuthor() {
        return Author.getAuthorInfo(materialType, modification);
    }

    @Override public String toString() {
        return "ModificationForPipeline{" +
                "pipelineId=" + pipelineId +
                ", modification=" + modification +
                '}';
    }

    public String getMaterialFingerprint() {
        return materialFingerprint;
    }
}
