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
package com.thoughtworks.go.server.presentation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FetchArtifactViewHelperTest {
    @Mock
    private SystemEnvironment systemEnvironment;
    private CruiseConfig cruiseConfig;


    /**
     *  PIPELINES                                          +           TEMPLATE
     *                                                     +
     * random_other                                        +
     * [mingle]                                            +
     *                                                     +           template-1
     * random_pipeline                                     +            [1, 2, 3]
     * [mingle]<---------------------------|               +
     *                                     |               +
     * uppest                              |               +
     * [1, 2, 3]                           |               +           template-2
     *     ^                               |               +            [1, 2]
     *     |                               |               +
     * upper                               |               +
     * [1, 2, 3]                           |               +
     *     ^<--------------------------|   |               +
     *     |                           |   |               +
     * downer                          |   |               +
     * [1, 2, 3]                       |   |               +
     *     ^<----------------------|   |   |               +
     *     |                       |   |   |               +
     * downest                    downest_wmp              +
     * [1, 2, 3]                    [1, 2, 3]              +
     *                                                     +
     */
    @Before
    public void setUp() {
        initMocks(this);
        when(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).thenReturn(true);

        when(systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)).thenReturn(true);

        PipelineConfig random = PipelineConfigMother.pipelineConfig("random_pipeline");
        JobConfigs randomJobs = random.getStage(new CaseInsensitiveString("mingle")).getJobs();
        randomJobs.add(new JobConfig("mingle_job"));

        PipelineConfig uppest = PipelineConfigMother.createPipelineConfig("uppest", "uppest_stage_1", "uppest_job_1", "uppest_job_1a");
        uppest.add(StageConfigMother.custom("uppest_stage_2", "uppest_job_2"));
        uppest.add(StageConfigMother.custom("uppest_stage_3", "uppest_job_3"));
        uppest.getStage("uppest_stage_1").getJobs().getJob(new CaseInsensitiveString("uppest_job_1")).artifactTypeConfigs().add(new PluggableArtifactConfig("a1", "hub"));

        PipelineConfig upper = PipelineConfigMother.createPipelineConfig("upper", "upper_stage_1", "upper_job_1", "upper_job_1a");
        upper.add(StageConfigMother.custom("upper_stage_2", "upper_job_2"));
        upper.add(StageConfigMother.custom("upper_stage_3", "upper_job_3"));
        upper.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("uppest_stage_2")));

        PipelineConfig downer = PipelineConfigMother.createPipelineConfig("downer", "downer_stage_1", "downer_job_1", "downer_job_1a");
        downer.add(StageConfigMother.custom("downer_stage_2", "downer_job_2"));
        downer.add(StageConfigMother.custom("downer_stage_3", "downer_job_3"));
        downer.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("upper"), new CaseInsensitiveString("upper_stage_2")));

        PipelineConfig downest = PipelineConfigMother.createPipelineConfig("downest", "downest_stage_1", "downest_job_1", "downest_job_1a");
        downest.add(StageConfigMother.custom("downest_stage_2", "downest_job_2"));
        downest.add(StageConfigMother.custom("downest_stage_3", "downest_job_3"));
        downest.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("downer"), new CaseInsensitiveString("downer_stage_2")));

        PipelineConfig downestWithMultiplePaths = PipelineConfigMother.createPipelineConfig("downest_wmp", "downest_wmp_stage_1", "downest_wmp_job_1", "downest_wmp_job_1a");
        downestWithMultiplePaths.add(StageConfigMother.custom("downest_wmp_stage_2", "downest_wmp_job_2"));
        downestWithMultiplePaths.add(StageConfigMother.custom("downest_wmp_stage_3", "downest_wmp_job_3"));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("downer"), new CaseInsensitiveString("downer_stage_2")));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("upper"), new CaseInsensitiveString("upper_stage_2")));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("random_pipeline"), new CaseInsensitiveString("mingle")));

        PipelineConfig randomOther = PipelineConfigMother.pipelineConfig("randomOther_pipeline");
        JobConfigs randomOtherJobs = randomOther.getStage(new CaseInsensitiveString("mingle")).getJobs();
        randomOtherJobs.add(new JobConfig("mingle_job_from_other_pipeline"));

        TemplatesConfig templates = new TemplatesConfig(
                new PipelineTemplateConfig(new CaseInsensitiveString("template-1"),
                        StageConfigMother.custom("t1-stage-1", "t1-job-1", "t1-job-1a"),
                        StageConfigMother.custom("t1-stage-2", "t1-job-2"),
                        StageConfigMother.custom("t1-stage-3", "t1-job-3")),
                new PipelineTemplateConfig(new CaseInsensitiveString("template-2"), StageConfigMother.custom("t2-stage-1", "t2-job-1", "t2-job-1a"), StageConfigMother.custom("t2-stage-2", "t2-job-2")));

        cruiseConfig = configWith(uppest, upper, downer, downest, downestWithMultiplePaths, random, randomOther);
        cruiseConfig.setTemplates(templates);
        cruiseConfig.getArtifactStores().add(new ArtifactStore("hub", "docker.plugin.id"));
    }

    @Test
    public void shouldNotSuggestIfTurnedOff() {
        when(systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)).thenReturn(false);

        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("uppest"), new CaseInsensitiveString("uppest_stage_3"), false).autosuggestMap();

        assertThat(jobForFetchHierarchy.isEmpty(), is(true));
    }

    @Test
    public void shouldNotSuggestForTemplatesIfToggleIsTurnedOff() {
        when(systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)).thenReturn(true);
        when(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).thenReturn(false);
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("templateName"), new CaseInsensitiveString("template_stage_3"), true).autosuggestMap();

        assertThat(jobForFetchHierarchy.isEmpty(), is(true));
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromLocalPipeline() {
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("uppest"), new CaseInsensitiveString("uppest_stage_3"), false).autosuggestMap();

        assertThat(jobForFetchHierarchy, is(
                m(str("uppest"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str(""),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())))));
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromDirectUpstream() {
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("upper"), new CaseInsensitiveString("upper_stage_3"), false).autosuggestMap();

        assertThat(jobForFetchHierarchy, is(
                m(str("uppest"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("upper"),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m())),
                  str(""),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m())))));
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromAncestorUpstream() {
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("downest"), new CaseInsensitiveString("downest_stage_3"), false).autosuggestMap();

        assertThat(jobForFetchHierarchy, is(
                m(str("uppest/upper/downer"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("upper/downer"),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m())),
                  str("downer"),
                        m(str("downer_stage_1"),
                                m(str("downer_job_1"), m(), str("downer_job_1a"), m()),
                          str("downer_stage_2"),
                                m(str("downer_job_2"), m())),
                  str("downest"),
                        m(str("downest_stage_1"),
                                m(str("downest_job_1"), m(), str("downest_job_1a"), m()),
                          str("downest_stage_2"),
                                m(str("downest_job_2"), m())),
                  str(""),
                        m(str("downest_stage_1"),
                                m(str("downest_job_1"), m(), str("downest_job_1a"), m()),
                          str("downest_stage_2"),
                                m(str("downest_job_2"), m())))));
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromAncestorUpstream_withMultiplePathsToSameDestination() {
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("downest_wmp"), new CaseInsensitiveString("downest_wmp_stage_3"), false).autosuggestMap();


        assertThat(jobForFetchHierarchy, is(
                m(str("uppest/upper/downer"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("uppest/upper"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("upper"),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m())),
                  str("upper/downer"),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m())),
                  str("downer"),
                        m(str("downer_stage_1"),
                                m(str("downer_job_1"), m(), str("downer_job_1a"), m()),
                          str("downer_stage_2"),
                                m(str("downer_job_2"), m())),
                  str("random_pipeline"),
                        m(str("mingle"),
                                m(str("mingle_job"), m())),
                  str("downest_wmp"),
                        m(str("downest_wmp_stage_1"),
                                m(str("downest_wmp_job_1"), m(), str("downest_wmp_job_1a"), m()),
                          str("downest_wmp_stage_2"),
                                m(str("downest_wmp_job_2"), m())),
                  str(""),
                        m(str("downest_wmp_stage_1"),
                                m(str("downest_wmp_job_1"), m(), str("downest_wmp_job_1a"), m()),
                          str("downest_wmp_stage_2"),
                                m(str("downest_wmp_job_2"), m())))));
    }

    @Test
    public void shouldSuggest_AllPathsInEntireConfigDependencyGraph_fromAnyStageToAnyUpstreamStage() {
        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString("template-1"), new CaseInsensitiveString("t1-stage-3"), true).autosuggestMap();

        assertThat(jobForFetchHierarchy, is(
                m(str("uppest/upper/downer/downest"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("uppest/upper/downer/downest_wmp"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("uppest/upper/downer"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("uppest/upper"),
                              m(str("uppest_stage_1"),
                                      m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                                str("uppest_stage_2"),
                                      m(str("uppest_job_2"), m())),
                  str("uppest/upper/downest_wmp"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m())),
                  str("uppest"),
                        m(str("uppest_stage_1"),
                                m(str("uppest_job_1"), m("a1", "docker.plugin.id"), str("uppest_job_1a"), m()),
                          str("uppest_stage_2"),
                                m(str("uppest_job_2"), m()),
                          str("uppest_stage_3"),
                                m(str("uppest_job_3"), m())),
                  str("upper/downer/downest_wmp"),
                              m(str("upper_stage_1"),
                                      m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                                str("upper_stage_2"),
                                      m(str("upper_job_2"), m())),
                  str("upper/downer/downest"),
                              m(str("upper_stage_1"),
                                      m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                                str("upper_stage_2"),
                                      m(str("upper_job_2"), m())),
                  str("upper/downer"),
                              m(str("upper_stage_1"),
                                      m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                              str("upper_stage_2"),
                                      m(str("upper_job_2"), m())),
                  str("upper/downest_wmp"),
                              m(str("upper_stage_1"),
                                      m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                              str("upper_stage_2"),
                                      m(str("upper_job_2"), m())),
                  str("upper"),
                        m(str("upper_stage_1"),
                                m(str("upper_job_1"), m(), str("upper_job_1a"), m()),
                          str("upper_stage_2"),
                                m(str("upper_job_2"), m()),
                          str("upper_stage_3"),
                                m(str("upper_job_3"), m())),
                  str("downer/downest_wmp"),
                        m(str("downer_stage_1"),
                                m(str("downer_job_1"), m(),  str("downer_job_1a"), m()),
                          str("downer_stage_2"),
                                m(str("downer_job_2"), m())),
                  str("downer/downest"),
                        m(str("downer_stage_1"),
                                m(str("downer_job_1"), m(), str("downer_job_1a"), m()),
                          str("downer_stage_2"),
                                m(str("downer_job_2"), m())),
                  str("downer"),
                        m(str("downer_stage_1"),
                                m(str("downer_job_1"), m(), str("downer_job_1a"), m()),
                          str("downer_stage_2"),
                                m(str("downer_job_2"), m()),
                          str("downer_stage_3"),
                                m(str("downer_job_3"), m())),
                  str("random_pipeline"),
                        m(str("mingle"),
                                m(str("mingle_job"), m())),
                  str("random_pipeline/downest_wmp"),
                        m(str("mingle"),
                                m(str("mingle_job"), m())),
                  str("downest"),
                        m(str("downest_stage_1"),
                                m(str("downest_job_1"), m(), str("downest_job_1a"), m()),
                          str("downest_stage_2"),
                                m(str("downest_job_2"), m()),
                          str("downest_stage_3"),
                                m(str("downest_job_3"), m())),
                  str("downest_wmp"),
                        m(str("downest_wmp_stage_1"),
                                m(str("downest_wmp_job_1"), m(), str("downest_wmp_job_1a"), m()),
                          str("downest_wmp_stage_2"),
                                m(str("downest_wmp_job_2"), m()),
                          str("downest_wmp_stage_3"),
                                m(str("downest_wmp_job_3"), m())),
                  str("randomOther_pipeline"),
                        m(str("mingle"),
                                m(str("mingle_job_from_other_pipeline"), m())),
                  str(""),
                        m(str("t1-stage-1"),
                                m(str("t1-job-1"), m(), str("t1-job-1a"), m()),
                          str("t1-stage-2"),
                                m(str("t1-job-2"), m())))));
    }


    @Test
    public void shouldProvideNoAutoSuggestionWhenThereIsOnlyOneTemplateAndAPipelineIsUsingThatTemplate() {
        when(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)).thenReturn(true);

        cruiseConfig = new BasicCruiseConfig();

        String templateName = "template-for-pipeline-my-pipeline";
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString(templateName),
                StageConfigMother.custom("stage-1", "stage1-job-1"));

        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("my-pipeline");
        pipeline.setTemplateName(templateName);

        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipeline("first", pipeline);

        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString(templateName), new CaseInsensitiveString("stage-1"), true).autosuggestMap();

        assertTrue(jobForFetchHierarchy.isEmpty());
    }

    @Test
    public void shouldProvideAutoSuggestionForTemplateOfPipelinesWhichAreNotUsingThatTemplate() {
        when(systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()).thenReturn(true);
        when(systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)).thenReturn(true);

        cruiseConfig = new BasicCruiseConfig();

        String templateName = "template-for-pipeline-my-pipeline";
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString(templateName),
                StageConfigMother.custom("stage-1", "stage1-job-1"));

        PipelineConfig pipelineUsingTemplate = new PipelineConfig();
        pipelineUsingTemplate.setName("my-pipeline");
        pipelineUsingTemplate.setTemplateName(templateName);

        PipelineConfig normalPipeline = PipelineConfigMother.createPipelineConfig("downest", "downest_stage_1", "downest_job_1", "downest_job_1a");

        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipeline("first", pipelineUsingTemplate);
        cruiseConfig.addPipeline("first", normalPipeline);

        HashMap<CaseInsensitiveString, Map> jobForFetchHierarchy = new FetchArtifactViewHelper(systemEnvironment, cruiseConfig, new CaseInsensitiveString(templateName), new CaseInsensitiveString("stage-1"), true).autosuggestMap();

        assertThat(jobForFetchHierarchy, is(m(str("downest"), m(str("downest_stage_1"), m(str("downest_job_1"), m(), str("downest_job_1a"), m())))));
    }
}
