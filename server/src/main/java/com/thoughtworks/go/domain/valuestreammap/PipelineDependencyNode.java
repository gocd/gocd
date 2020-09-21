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
package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.*;

public class PipelineDependencyNode extends Node {
    private Set<Revision> revisions = new HashSet<>();
    private String message;
    private boolean canEdit;
    private String templateName;

    public PipelineDependencyNode(CaseInsensitiveString nodeId, String nodeName) {
        super(DependencyNodeType.PIPELINE, nodeId, nodeName);
    }

    @Override
    public void addRevision(Revision revision) {
        if (revision != null) {
            revisions.add(revision);
        }
    }

    private void emptyRevisions() {
        revisions = new HashSet<>();
    }

    @Override
    public List<Revision> revisions() {
        ArrayList<Revision> revisions = new ArrayList<>(this.revisions);
        revisions.sort((o1, o2) -> ((PipelineRevision) o1).compareTo((PipelineRevision) o2));
        return revisions;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public void addRevisions(List<Revision> revisions) {
        this.revisions.addAll(revisions);
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageString() {
        return message;
    }

    public void setNoPermission() {
        emptyRevisions();
        setMessage("You are not authorized to view this pipeline.");
        setViewType(VSMViewType.NO_PERMISSION);
    }

    public void setDeleted() {
        emptyRevisions();
        setMessage("Pipeline has been deleted.");
        setViewType(VSMViewType.DELETED);
    }
}
