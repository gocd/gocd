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
package com.thoughtworks.go.server.view.velocity;

import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.service.ArtifactsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static com.thoughtworks.go.domain.buildcause.BuildCause.createWithModifications;
import static com.thoughtworks.go.helper.JobInstanceMother.building;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.helper.PipelineMother.schedule;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildDetailPageVelocityTemplateTest {

    @Test
    public void shouldEscapeBuildCauseOnVelocityTemplate() throws Exception {
        Document actualDoc = Jsoup.parse(getBuildDetailVelocityView(createJobDetailModel()).render());
        assertThat(actualDoc.select("#build-detail-summary").last().html(), containsString("modified by Ernest Hemingway &lt;oldman@sea.com&gt;"));
    }

    @Test
    public void shouldEscapeBuildCauseInTrimpathTemplate() throws Exception {
        Document actualDoc = Jsoup.parse(getBuildDetailVelocityView(createJobDetailModel()).render());
        assertThat(actualDoc.select("#build-summary-template").last().html(), containsString("modified by Ernest Hemingway &amp;lt;oldman@sea.com&amp;gt;"));
    }

    @Test
    public void shouldRenderIframeSandboxByDefaultForTestsTab() throws IOException {
        HashMap<String, Object> data = new HashMap<>();
        JobDetailPresentationModel jobDetailPresentationModel = mock(JobDetailPresentationModel.class);
        data.put("presenter", jobDetailPresentationModel);
        data.put("useIframeSandbox", true);
        when(jobDetailPresentationModel.hasTests()).thenReturn(true);
        Document actualDoc = Jsoup.parse(getBuildDetailVelocityView(data).render());

        assertThat(actualDoc.select("#tab-content-of-tests").last().html(), containsString("<iframe sandbox=\"allow-scripts\""));
    }

    @Test
    public void shouldRenderANormalIframeForTestsTabIfUserHasDisabledSandbox() throws IOException {
        HashMap<String, Object> data = new HashMap<>();
        JobDetailPresentationModel jobDetailPresentationModel = mock(JobDetailPresentationModel.class);
        data.put("presenter", jobDetailPresentationModel);
        data.put("useIframeSandbox", false);
        when(jobDetailPresentationModel.hasTests()).thenReturn(true);
        Document actualDoc = Jsoup.parse(getBuildDetailVelocityView(data).render());

        assertThat(actualDoc.select("#tab-content-of-tests").last().html(), containsString("<iframe src="));
    }

    private HashMap<String, Object> createJobDetailModel() {
        GitMaterialConfig gitMaterialConfig = gitMaterialConfig();

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(new GitMaterial(gitMaterialConfig),
                new Modification("Ernest Hemingway <oldman@sea.com>", "comment", "email", new Date(), "12", ""));

        Pipeline pipeline = schedule(pipelineConfig("pipeline", new MaterialConfigs(gitMaterialConfig)), createWithModifications(materialRevisions, ""));
        JobDetailPresentationModel model = new JobDetailPresentationModel(building("job"), new JobInstances(), null,
                pipeline, null, new TrackingTool(),
                mock(ArtifactsService.class), StageMother.custom("stage"));

        HashMap<String, Object> data = new HashMap<>();
        data.put("presenter", model);
        return data;
    }

    private TestVelocityView getBuildDetailVelocityView(HashMap<String, Object> data) throws IOException {
        TestVelocityView view = new TestVelocityView("/WEB-INF/vm/build_detail/build_detail_page.vm", data);
        view.setupAdditionalRealTemplate("shared/_header.vm");
        view.setupAdditionalRealTemplate("shared/_footer.vm");
        view.setupAdditionalRealTemplate("shared/_flash_message.vm");
        view.setupAdditionalRealTemplate("shared/_artifacts.vm");
        view.setupAdditionalRealTemplate("shared/_artifact_entry.vm");
        view.setupAdditionalRealTemplate("shared/_package_material_revision_comment.vm");
        view.setupAdditionalRealTemplate("shared/_job_details_breadcrumbs.vm");

        view.setupAdditionalRealTemplate("sidebar/_sidebar_build_list.vm");

        view.setupAdditionalRealTemplate("build_detail/_buildoutput.vm");
        view.setupAdditionalRealTemplate("build_detail/_build_output_raw.vm");
        view.setupAdditionalRealTemplate("build_detail/_tests.vm");
        view.setupAdditionalRealTemplate("build_detail/_test_output_config.vm");
        view.setupAdditionalRealTemplate("build_detail/_artifacts.vm");
        view.setupAdditionalRealTemplate("build_detail/_materials.vm");
        view.setupAdditionalRealTemplate("build_detail/_properties.vm");
        view.setupAdditionalRealTemplate("build_detail/_material_revisions_jstemplate.vm");
        view.setupAdditionalRealTemplate("build_detail/_build_detail_summary_jstemplate.vm");

        return view;
    }
}
