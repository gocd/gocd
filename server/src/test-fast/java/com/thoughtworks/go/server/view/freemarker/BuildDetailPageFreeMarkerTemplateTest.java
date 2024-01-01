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
package com.thoughtworks.go.server.view.freemarker;

import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.service.ArtifactsService;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.buildcause.BuildCause.createWithModifications;
import static com.thoughtworks.go.helper.JobInstanceMother.building;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.helper.PipelineMother.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BuildDetailPageFreeMarkerTemplateTest extends AbstractFreemarkerTemplateTest {

    private static final String DODGY_LABEL = "label-1-<dodgy>-hello('world')";
    private static final String DODGY_LABEL_UNICODE_ESCAPED = "label-1-\\u003cdodgy\\u003e-hello(\\u0027world\\u0027)";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp("build_detail/build_detail_page.ftlh");
    }

    @Test
    public void shouldEscapeBuildCauseForHtml() {
        Document actualDoc = parser.parseInput(view.render(createJobDetailModel()), "");
        assertThat(actualDoc.select("#build-detail-summary").last().html())
            .contains("modified by Ernest Hemingway &lt;oldman@sea.com&gt;");
    }

    @Test
    public void shouldEscapeBuildCauseForJavaScriptViaTrimPathTemplate() {
        Document actualDoc = parser.parseInput(view.render(createJobDetailModel()), "");
        assertThat(actualDoc.select("#build-summary-template").last().html())
            .contains("modified by Ernest Hemingway &amp;lt;oldman@sea.com&amp;gt;");
    }

    @Test
    public void shouldEscapeRenderedPipelineLabels() {
        Document actualDoc = parser.parseInput(view.render(createJobDetailModel()), "");

        // Header is escaped
        assertThat(actualDoc.select("#job_details_header").last().html())
            .contains(StringEscapeUtils.escapeHtml4(DODGY_LABEL));

        // Sidebar links are escaped
        assertThat(actualDoc.select("#buildlist-container ul li[id=build_list_1]").last().html())
            .contains(String.format("pipeline/%s/stage/1/job", StringEscapeUtils.escapeHtml4(DODGY_LABEL)));

        // Strange JS that updates sidebar should be correctly JS escaped
        assertThat(actualDoc.select("#build_history_holder script").last().html())
            .contains("json_to_css.update_build_list(eval({\"building_info\"")
            .contains(String.format("\"buildLocatorForDisplay\":\"pipeline/%s/stage/1/job\"", DODGY_LABEL_UNICODE_ESCAPED));
    }

    @Test
    public void shouldRenderArtifactFilesAsRawHtml() {
        Document actualDoc = parser.parseInput(view.render(createJobDetailModel()), "");
        assertThat(actualDoc.select("#tab-content-of-artifacts .files").last().html())
            .contains("<a href=\"null\"> console.log </a>");
    }

    @Test
    public void shouldRenderIframeSandboxForTestsTab() {
        JobDetailPresentationModel jobDetailPresentationModel = mock(JobDetailPresentationModel.class, RETURNS_SMART_NULLS);
        when(jobDetailPresentationModel.hasTests()).thenReturn(true);
        when(jobDetailPresentationModel.getCustomizedTabs()).thenReturn(new Tabs());

        Document actualDoc = parser.parseInput(view.render(minimalModelFrom(jobDetailPresentationModel)), "");
        assertThat(actualDoc.select("#tab-content-of-tests").last().html())
            .contains("<iframe sandbox=\"allow-scripts\"");
    }

    private Map<String, Object> createJobDetailModel() {
        GitMaterialConfig gitMaterialConfig = gitMaterialConfig();

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(new GitMaterial(gitMaterialConfig),
            new Modification("Ernest Hemingway <oldman@sea.com>", "comment", "email", new Date(), "12", ""));

        Pipeline pipeline = schedule(pipelineConfig("pipeline", new MaterialConfigs(gitMaterialConfig)), createWithModifications(materialRevisions, ""));
        JobInstance jobInstance = building("job");
        jobInstance.getIdentifier().setPipelineLabel(DODGY_LABEL);

        JobInstances recent25 = new JobInstances();
        recent25.add(jobInstance);
        JobDetailPresentationModel model = new JobDetailPresentationModel(jobInstance, recent25,
            pipeline, new Tabs(), new TrackingTool(),
            mock(ArtifactsService.class), StageMother.custom("stage"));

        return minimalModelFrom(model);
    }

    private static Map<String, Object> minimalModelFrom(JobDetailPresentationModel jobDetailPresentationModel) {
        Map<String, Object> data = new HashMap<>();
        data.put("presenter", jobDetailPresentationModel);
        data.put("isAgentAlive", false);
        data.put("websocketEnabled", false);
        return data;
    }

}
