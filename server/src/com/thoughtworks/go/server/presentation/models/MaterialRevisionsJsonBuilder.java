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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.ModificationVisitorAdapter;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.DateUtils;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class MaterialRevisionsJsonBuilder extends ModificationVisitorAdapter {
    private JsonList materials = new JsonList();
    private JsonMap materialJson;
    private JsonList modificationsJson;
    private JsonList modifiedFilesJson;
    private boolean includeModifiedFiles = true;
    private final CommentRenderer commentRenderer;
    private MaterialRevision revision;

    public MaterialRevisionsJsonBuilder(CommentRenderer commentRenderer) {
        this.commentRenderer = commentRenderer;
    }

    public void visit(MaterialRevision revision) {
        this.revision = revision;
        modificationsJson = new JsonList();

        materialJson = new JsonMap();
        materialJson.put("revision", revision.getRevision().getRevision());
        materialJson.put("revision_href", revision.getRevision().getRevisionUrl());
        materialJson.put("user", revision.buildCausedBy());
        materialJson.put("date", DateUtils.formatISO8601(revision.getDateOfLatestModification()));
        materialJson.put("changed", String.valueOf(revision.isChanged()));
        materialJson.put("modifications", modificationsJson);

        materials.add(materialJson);
    }

    public void visit(Material material, Revision revision) {
        material.toJson(materialJson, revision);
    }

    public void visit(Modification modification) {
        modifiedFilesJson = new JsonList();

        JsonMap jsonMap = new JsonMap();
        jsonMap.put("user", escapeHtml(modification.getUserDisplayName()));
        jsonMap.put("revision", modification.getRevision());
        jsonMap.put("date", DateUtils.formatISO8601(modification.getModifiedTime()));
        String comment = modification.getComment();
        if (!revision.getMaterial().getType().equals(PackageMaterial.TYPE)) {
            comment = commentRenderer.render(comment);
        }
        jsonMap.put("comment", comment);
        jsonMap.put("modifiedFiles", modifiedFilesJson);

        modificationsJson.add(jsonMap);
    }

    public void visit(ModifiedFile file) {
        if (!includeModifiedFiles) {
            return;
        }

        JsonMap jsonMap = new JsonMap();
        jsonMap.put("action", file.getAction().toString());
        jsonMap.put("fileName", file.getFileName());

        modifiedFilesJson.add(jsonMap);
    }

    public JsonList json() {
        return materials;
    }

    public void setIncludeModifiedFiles(boolean includeModifiedFiles) {
        this.includeModifiedFiles = includeModifiedFiles;
    }
}
