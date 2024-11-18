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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
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
            .isArray().hasSize(1)
            .first()
            .and(
                a -> a.node("scmType").isEqualTo("Subversion"),
                a -> a.node("location").isEqualTo("http://url"),
                a -> a.node("folder").isEqualTo("svn-folder"),
                a -> a.node("revision").isString().isEqualTo(expectedRevision),
                a -> a.node("user").isEqualTo(ModificationsMother.MOD_USER_WITH_HTML_CHAR),
                a -> a.node("date").isEqualTo(expectedDate)
        );
    }

    @Test
    public void shouldShowEmptyFolderInJson() {
        svnMaterial.setFolder(null);

        String jsonRevisions = buildJson();
        assertThatJson(jsonRevisions)
            .isArray().hasSize(1)
            .first()
            .node("folder").isEqualTo("");
    }

    @Test
    public void shouldShowEachModificationInJson() {
        String jsonRevisions = buildJson();
        assertThatJson(jsonRevisions)
            .isArray().hasSize(1)
            .first()
            .node("modifications").isArray().hasSize(3)
            .last()
            .and(
                a -> a.node("user").isEqualTo(ModificationsMother.MOD_USER),
                a -> a.node("comment").isEqualTo(ModificationsMother.MOD_COMMENT),
                a -> a.node("date").isEqualTo(DateUtils.formatISO8601(ModificationsMother.TWO_DAYS_AGO_CHECKIN)),
                a -> a.node("modifiedFiles").isArray().hasSize(1)
                    .first()
                    .and(
                        b -> b.node("action").isEqualTo(ModificationsMother.MOD_MODIFIED_ACTION.toString()),
                        b -> b.node("fileName").isEqualTo(ModificationsMother.MOD_FILE_BUILD_XML)
                    )
            );
    }

    @Test
    public void shouldEscapeUsername() {
        String jsonRevisions = buildJson();

        assertThatJson(jsonRevisions)
            .and(
                a -> a.node("[0].modifications[0].user").isEqualTo(ModificationsMother.MOD_USER_WITH_HTML_CHAR),
                a -> a.node("[0].modifications[0].comment").isEqualTo(escapeHtml4(ModificationsMother.MOD_COMMENT_3))
            );
    }

    @Test
    public void shouldContainModificationChanged() {
        materialRevisions.getMaterialRevision(0).markAsChanged();

        assertThatJson(buildJson())
            .isArray().hasSize(1)
            .first()
            .node("changed").isString().isEqualTo("true");
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
