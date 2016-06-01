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

package com.thoughtworks.go.domain.valuestreammap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;

public class PipelineDependencyNode extends Node {
    private Set<Revision> revisions = new HashSet<>();
    private Localizable message;

    public PipelineDependencyNode(String nodeId, String nodeName) {
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
        Collections.sort(revisions, new Comparator<Revision>() {
            @Override
            public int compare(Revision o1, Revision o2) {
                return ((PipelineRevision) o1).compareTo((PipelineRevision) o2);
            }
        });
        return revisions;
    }

    @Override
    public void addRevisions(List<Revision> revisions) {
        this.revisions.addAll(revisions);
    }

    @Override
    public void setMessage(Localizable message) {
        this.message = message;
    }

    public Localizable getMessage() {
        return message;
    }

    @Override
    public String getMessageString(Localizer localizer) {
        if(message == null)
            return null;
        return message.localize(localizer);
    }

    public void setNoPermission() {
        emptyRevisions();
        setMessage(LocalizedMessage.string("VSM_PIPELINE_UNAUTHORIZED"));
        setViewType(VSMViewType.NO_PERMISSION);
    }

    public void setDeleted() {
        emptyRevisions();
        setMessage(LocalizedMessage.string("VSM_PIPELINE_DELETED"));
        setViewType(VSMViewType.DELETED);
    }
}
