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

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONObject;
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
import com.thoughtworks.go.util.JsonUtils;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MaterialRevisionsJsonBuilderTest {
    private MaterialRevisions materialRevisions;
    private SvnMaterial svnMaterial;
    private MaterialRevisionsJsonBuilder builder;

    @Before
    public void setUp() {
        svnMaterial = MaterialsMother.svnMaterial("http://url", "svn-folder");

        materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(svnMaterial, ModificationsMother.multipleModificationList());
        builder = new MaterialRevisionsJsonBuilder(new TrackingTool());
    }

    private JSONArray buildJson() {
        materialRevisions.accept(builder);
        List json = builder.json();
        return (JSONArray) JsonUtils.parseJsonValue(json);
    }

    @Test
    public void shouldShowEachMaterialInJson() {
        JSONArray revisions = buildJson();

        assertThat(revisions.size(), is(1));
        JSONObject jsonValue = (JSONObject) revisions.get(0);
        assertThat(jsonValue.get("scmType").render(false), is("\"Subversion\""));
        assertThat(jsonValue.get("location").render(false), is("\"http://url\""));
        assertThat(jsonValue.get("folder").render(false), is("\"svn-folder\""));

        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        String revision = materialRevision.getRevision().getRevision();
        assertThat(jsonValue.get("revision").render(false), is("\"" + revision + "\""));
        assertThat(jsonValue.get("action").render(false), is("\"Modified\""));
        assertThat(jsonValue.get("user").render(false), is("\"" + ModificationsMother.MOD_USER_WITH_HTML_CHAR + "\""));
        String date = DateUtils.formatISO8601(materialRevision.getDateOfLatestModification());
        assertThat(jsonValue.get("date").render(false), is("\"" + date + "\""));
    }

    @Test
    public void shouldShowEmptyFolderInJson() {
        svnMaterial.setFolder(null);
        JSONArray revisions = buildJson();
        assertThat(revisions.size(), is(1));
        JSONObject jsonValue = (JSONObject) revisions.get(0);
        assertThat(jsonValue.get("folder").render(false), is("\"\""));
    }

    @Test
    public void shouldShowEachModificationInJson() {
        JSONArray revisions = buildJson();
        JSONObject revision = (JSONObject) revisions.get(0);

        JSONArray jsonArray = (JSONArray) revision.get("modifications");
        assertThat(jsonArray.size(), is(3));

        JSONObject modification = (JSONObject) jsonArray.get(2);
        assertAttributeInJson(modification, "user", ModificationsMother.MOD_USER);
        assertAttributeInJson(modification, "comment", ModificationsMother.MOD_COMMENT);
        assertAttributeInJson(modification, "date", DateUtils.formatISO8601(ModificationsMother.TWO_DAYS_AGO_CHECKIN));

        JSONArray modifiedFiles = (JSONArray) modification.get("modifiedFiles");
        assertThat(modifiedFiles.size(), is(1));

        JSONObject file = (JSONObject) modifiedFiles.get(0);
        assertAttributeInJson(file, "action", ModificationsMother.MOD_MODIFIED_ACTION.toString());
        assertAttributeInJson(file, "fileName", ModificationsMother.MOD_FILE_BUILD_XML);
    }

    @Test
    public void shouldEscapeUsername() throws Exception {
        JSONArray revisions = buildJson();
        JSONObject revision = (JSONObject) revisions.get(0);
        JSONArray jsonArray = (JSONArray) revision.get("modifications");
        JSONObject modification = (JSONObject) jsonArray.get(0);

        assertAttributeInJson(modification, "user", ModificationsMother.MOD_USER_WITH_HTML_CHAR);
        assertAttributeInJson(modification, "comment", escapeHtml(ModificationsMother.MOD_COMMENT_3));
    }

    @Test
    public void shouldContainModificationChanged() throws Exception {
        materialRevisions.getMaterialRevision(0).markAsChanged();
        JSONArray revisions = buildJson();
        JSONObject revision = (JSONObject) revisions.get(0);
        JSONArray jsonArray = (JSONArray) revision.get("modifications");

        assertAttributeInJson(revision, "changed", "true");
    }

    @Test
    public void shouldRenderDependencyMaterialRevision() {
        String revision = "cruise/10/dev/1";
        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("cruise"), new CaseInsensitiveString("dev")), new Modification(new Date(), revision, "MOCK_LABEL-12", null)));
        MaterialRevisionsJsonBuilder jsonBuilder = new MaterialRevisionsJsonBuilder(new TrackingTool());
        revisions.accept(jsonBuilder);
        JsonValue revisionsJson = JsonUtils.from(jsonBuilder.json());
        assertThat(revisionsJson.getString(0, "revision_href"), is("pipelines/" + revision));
    }

    @Test
    public void shouldNotProcessPackageMaterialComment() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("TYPE", "PACKAGE_MATERIAL");
        map.put("TRACKBACK_URL", "google.com");
        map.put("COMMENT", "comment");
        String packageMaterialComment = JsonHelper.toJsonString(map);
        Modification modification = new Modification("user", packageMaterialComment, "some@com", new Date(), "1234");
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.packageMaterial(), modification));
        JSONArray jsonArray = buildJson();
        JSONObject modificationJson = (JSONObject) ((JSONArray) ((JSONObject) jsonArray.get(0)).get("modifications")).get(0);
        assertThat(modificationJson.get("comment").render(false), is(JsonHelper.toJsonString(packageMaterialComment)));
    }

    private void assertAttributeInJson(JSONObject jsonObject, String attributeName, String attributeValue) {
        assertThat(jsonObject.get(attributeName).render(false), is("\"" + attributeValue + "\""));
    }

}
