/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class MaterialRevisionsJsonBuilderTest {
    private MaterialRevisions materialRevisions;
    private SvnMaterial svnMaterial;
    private MaterialRevisionsJsonBuilder builder;

    @BeforeEach
    public void setUp() {
        svnMaterial = MaterialsMother.svnMaterial("http://url", "svn-folder");

        materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(svnMaterial, ModificationsMother.multipleModificationList());
        builder = new MaterialRevisionsJsonBuilder(new TrackingTool());
    }

    private String buildJson() {
        materialRevisions.accept(builder);
        List json = builder.json();
        return new Gson().toJson(json);
    }

    @Test
    public void shouldShowEachMaterialInJson() {
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        String expectedRevision = materialRevision.getRevision().getRevision();
        String expectedDate = DateUtils.formatISO8601(materialRevision.getDateOfLatestModification());

        String jsonRevisions = buildJson();
        assertThatJson(jsonRevisions)
            .isArray()
            .ofLength(1);

        assertThatJson(jsonRevisions)
            .node("[0].scmType").isEqualTo("Subversion")
            .node("[0].location").isEqualTo("http://url")
            .node("[0].folder").isEqualTo("svn-folder")
            .node("[0].revision").isStringEqualTo(expectedRevision)
            .node("[0].user").isEqualTo(ModificationsMother.MOD_USER_WITH_HTML_CHAR)
            .node("[0].date").isEqualTo(expectedDate);
    }

    @Test
    public void shouldShowEmptyFolderInJson() {
        svnMaterial.setFolder(null);

        String jsonRevisions = buildJson();
        assertThatJson(jsonRevisions)
            .isArray()
            .ofLength(1);

        assertThatJson(jsonRevisions)
            .node("[0].folder").isEqualTo("");
    }

    @Test
    public void shouldShowEachModificationInJson() {
        String jsonRevisions = buildJson();
        assertThatJson(jsonRevisions)
            .node("[0].modifications")
            .isArray()
            .ofLength(3);

        assertThatJson(jsonRevisions)
            .node("[0].modifications[2].user").isEqualTo(ModificationsMother.MOD_USER)
            .node("[0].modifications[2].comment").isEqualTo(ModificationsMother.MOD_COMMENT)
            .node("[0].modifications[2].date").isEqualTo(DateUtils.formatISO8601(ModificationsMother.TWO_DAYS_AGO_CHECKIN))
            .node("[0].modifications[2].modifiedFiles").isArray().ofLength(1);

        assertThatJson(jsonRevisions)
            .node("[0].modifications[2].modifiedFiles[0].action").isEqualTo(ModificationsMother.MOD_MODIFIED_ACTION.toString())
            .node("[0].modifications[2].modifiedFiles[0].fileName").isEqualTo(ModificationsMother.MOD_FILE_BUILD_XML);
    }

    @Test
    public void shouldEscapeUsername() {
        String jsonRevisions = buildJson();

        assertThatJson(jsonRevisions)
            .node("[0].modifications[0].user").isEqualTo(ModificationsMother.MOD_USER_WITH_HTML_CHAR)
            .node("[0].modifications[0].comment").isEqualTo(escapeHtml4(ModificationsMother.MOD_COMMENT_3));
    }

    @Test
    public void shouldContainModificationChanged() {
        materialRevisions.getMaterialRevision(0).markAsChanged();

        assertThatJson(buildJson())
            .node("[0].changed").isStringEqualTo("true");
    }

    @Test
    public void shouldRenderDependencyMaterialRevision() {
        String revision = "cruise/10/dev/1";
        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("cruise"), new CaseInsensitiveString("dev")), new Modification(new Date(), revision, "MOCK_LABEL-12", null)));
        MaterialRevisionsJsonBuilder jsonBuilder = new MaterialRevisionsJsonBuilder(new TrackingTool());
        revisions.accept(jsonBuilder);
        assertThatJson(jsonBuilder.json())
            .node("[0].revision_href").isEqualTo("pipelines/" + revision);
    }

    @Test
    public void shouldNotProcessPackageMaterialComment() {
        Map<String, String> map = new HashMap<>();
        map.put("TYPE", "PACKAGE_MATERIAL");
        map.put("TRACKBACK_URL", "google.com");
        map.put("COMMENT", "comment");
        String packageMaterialComment = JsonHelper.toJsonString(map);
        Modification modification = new Modification("user", packageMaterialComment, "some@com", new Date(), "1234");
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.packageMaterial(), modification));
        assertThatJson(buildJson())
            .node("[0].modifications[0].comment").isEqualTo(JsonHelper.toJsonString(packageMaterialComment));
    }
}
