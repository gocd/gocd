/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.ModificationVisitorAdapter;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.materials.PackageMaterial.TYPE;
import static com.thoughtworks.go.util.DateUtils.formatISO8601;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class MaterialRevisionsJsonBuilder extends ModificationVisitorAdapter {
    private List materials = new ArrayList();
    private Map<String, Object> materialJson;
    private List modificationsJson;
    private List modifiedFilesJson;
    private boolean includeModifiedFiles = true;
    private final CommentRenderer commentRenderer;
    private MaterialRevision revision;

    public MaterialRevisionsJsonBuilder(CommentRenderer commentRenderer) {
        this.commentRenderer = commentRenderer;
    }

    public void visit(MaterialRevision revision) {
        this.revision = revision;
        modificationsJson = new ArrayList();

        materialJson = new LinkedHashMap();
        materialJson.put("revision", revision.getRevision().getRevision());
        materialJson.put("revision_href", revision.getRevision().getRevisionUrl());
        materialJson.put("user", revision.buildCausedBy());
        materialJson.put("date", formatISO8601(revision.getDateOfLatestModification()));
        materialJson.put("changed", valueOf(revision.isChanged()));
        materialJson.put("modifications", modificationsJson);

        materials.add(materialJson);
    }

    public void visit(Material material, Revision revision) {
        material.toJson(materialJson, revision);
    }

    public void visit(Modification modification) {
        modifiedFilesJson = new ArrayList();

        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("user", modification.getUserDisplayName());
        jsonMap.put("revision", modification.getRevision());
        jsonMap.put("date", formatISO8601(modification.getModifiedTime()));
        String comment = modification.getComment();
        if (!revision.getMaterial().getType().equals(TYPE)) {
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

        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("action", file.getAction().toString());
        jsonMap.put("fileName", file.getFileName());

        modifiedFilesJson.add(jsonMap);
    }

    public List json() {
        return materials;
    }

    public void setIncludeModifiedFiles(boolean includeModifiedFiles) {
        this.includeModifiedFiles = includeModifiedFiles;
    }
}
