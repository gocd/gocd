/*
 * Copyright Thoughtworks, Inc.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.presentation.FetchArtifactViewHelper.PermissionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@ExtendWith(MockitoExtension.class)
public class FetchArtifactViewHelperTest {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final PermissionResolver ALL_VIEWABLE = name -> true;

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
    @BeforeEach
    public void setUp() {
        PipelineConfig random = PipelineConfigMother.pipelineConfig("random_pipeline");
        JobConfigs randomJobs = random.getStage(cis("mingle")).getJobs();
        randomJobs.add(new JobConfig("mingle_job"));

        PipelineConfig uppest = PipelineConfigMother.createPipelineConfig("uppest", "uppest_stage_1", "uppest_job_1", "uppest_job_1a");
        uppest.add(StageConfigMother.custom("uppest_stage_2", "uppest_job_2"));
        uppest.add(StageConfigMother.custom("uppest_stage_3", "uppest_job_3"));
        uppest.getStage("uppest_stage_1").getJobs().getJob(cis("uppest_job_1")).artifactTypeConfigs().add(new PluggableArtifactConfig("a1", "hub"));

        PipelineConfig upper = PipelineConfigMother.createPipelineConfig("upper", "upper_stage_1", "upper_job_1", "upper_job_1a");
        upper.add(StageConfigMother.custom("upper_stage_2", "upper_job_2"));
        upper.add(StageConfigMother.custom("upper_stage_3", "upper_job_3"));
        upper.addMaterialConfig(new DependencyMaterialConfig(cis("uppest"), cis("uppest_stage_2")));

        PipelineConfig downer = PipelineConfigMother.createPipelineConfig("downer", "downer_stage_1", "downer_job_1", "downer_job_1a");
        downer.add(StageConfigMother.custom("downer_stage_2", "downer_job_2"));
        downer.add(StageConfigMother.custom("downer_stage_3", "downer_job_3"));
        downer.addMaterialConfig(new DependencyMaterialConfig(cis("upper"), cis("upper_stage_2")));

        PipelineConfig downest = PipelineConfigMother.createPipelineConfig("downest", "downest_stage_1", "downest_job_1", "downest_job_1a");
        downest.add(StageConfigMother.custom("downest_stage_2", "downest_job_2"));
        downest.add(StageConfigMother.custom("downest_stage_3", "downest_job_3"));
        downest.addMaterialConfig(new DependencyMaterialConfig(cis("downer"), cis("downer_stage_2")));

        PipelineConfig downestWithMultiplePaths = PipelineConfigMother.createPipelineConfig("downest_wmp", "downest_wmp_stage_1", "downest_wmp_job_1", "downest_wmp_job_1a");
        downestWithMultiplePaths.add(StageConfigMother.custom("downest_wmp_stage_2", "downest_wmp_job_2"));
        downestWithMultiplePaths.add(StageConfigMother.custom("downest_wmp_stage_3", "downest_wmp_job_3"));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(cis("downer"), cis("downer_stage_2")));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(cis("upper"), cis("upper_stage_2")));
        downestWithMultiplePaths.addMaterialConfig(new DependencyMaterialConfig(cis("random_pipeline"), cis("mingle")));

        PipelineConfig randomOther = PipelineConfigMother.pipelineConfig("randomOther_pipeline");
        JobConfigs randomOtherJobs = randomOther.getStage(cis("mingle")).getJobs();
        randomOtherJobs.add(new JobConfig("mingle_job_from_other_pipeline"));

        TemplatesConfig templates = new TemplatesConfig(
            new PipelineTemplateConfig(cis("template-1"),
                StageConfigMother.custom("t1-stage-1", "t1-job-1", "t1-job-1a"),
                StageConfigMother.custom("t1-stage-2", "t1-job-2"),
                StageConfigMother.custom("t1-stage-3", "t1-job-3")),
            new PipelineTemplateConfig(cis("template-2"), StageConfigMother.custom("t2-stage-1", "t2-job-1", "t2-job-1a"), StageConfigMother.custom("t2-stage-2", "t2-job-2")));

        cruiseConfig = configWith(uppest, upper, downer, downest, downestWithMultiplePaths, random, randomOther);
        cruiseConfig.setTemplates(templates);
        cruiseConfig.getArtifactStores().add(new ArtifactStore("hub", "docker.plugin.id"));
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromLocalPipeline() {
        var targetPipeline = cis("uppest");
        var targetStage = cis("uppest_stage_3");
        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromDirectUpstream() {
        var targetPipeline = cis("upper");
        var targetStage = cis("upper_stage_3");
        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "upper": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromAncestorUpstream() {
        var targetPipeline = cis("downest");
        var targetStage = cis("downest_stage_3");
        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "upper/downer": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "downer": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} }
              },
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              },
              "": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldSuggest_AncestorFetch_validForFetchArtifactCall_fromAncestorUpstream_withMultiplePathsToSameDestination() {
        var targetPipeline = cis("downest_wmp");
        var targetStage = cis("downest_wmp_stage_3");
        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "upper": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "upper/downer": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "downer": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} }
              },
              "random_pipeline": {
                "mingle": { "mingle_job": {} }
              },
              "downest_wmp": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} }
              },
              "": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldSuggest_AllPathsInEntireConfigDependencyGraph_fromAnyStageToAnyUpstreamStage() {
        var targetTemplate = cis("template-1");
        var targetStage = cis("t1-stage-3");
        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} },
                "uppest_stage_3": { "uppest_job_3": {} }
              },
              "uppest/upper": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "upper": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} },
                "upper_stage_3": { "upper_job_3": {} }
              },
              "upper/downer": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "upper/downest_wmp": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "upper/downer/downest": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "upper/downer/downest_wmp": {
                "upper_stage_1": { "upper_job_1": {}, "upper_job_1a": {} },
                "upper_stage_2": { "upper_job_2": {} }
              },
              "downer": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} },
                "downer_stage_3": { "downer_job_3": {} }
              },
              "downer/downest": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} }
              },
              "downer/downest_wmp": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} }
              },
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} },
                "downest_stage_3": { "downest_job_3": {} }
              },
              "downest_wmp": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} },
                "downest_wmp_stage_3": { "downest_wmp_job_3": {} }
              },
              "random_pipeline": {
                "mingle": { "mingle_job": {} }
              },
              "random_pipeline/downest_wmp": {
                "mingle": { "mingle_job": {} }
              },
              "randomOther_pipeline": {
                "mingle": { "mingle_job_from_other_pipeline": {} }
              },
              "": {
                "t1-stage-1": { "t1-job-1": {}, "t1-job-1a": {} },
                "t1-stage-2": { "t1-job-2": {} }
              }
            }""");
    }


    @Test
    public void shouldProvideNoAutoSuggestionWhenThereIsOnlyOneTemplateAndAPipelineIsUsingThatTemplate() {
        var targetTemplate = cis("template-for-pipeline-my-pipeline");
        var targetStage = cis("stage-1");

        cruiseConfig = new BasicCruiseConfig();
        PipelineTemplateConfig template = new PipelineTemplateConfig(targetTemplate,
            StageConfigMother.custom(targetStage.toString(), "stage1-job-1"));

        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("my-pipeline");
        pipeline.setTemplateName(targetTemplate);

        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipeline("first", pipeline);

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("{}");
    }

    @Test
    public void shouldProvideAutoSuggestionForTemplateOfPipelinesWhichAreNotUsingThatTemplate() {
        var targetTemplate = cis("template-for-pipeline-my-pipeline");
        var targetStage = cis("stage-1");

        cruiseConfig = new BasicCruiseConfig();
        PipelineTemplateConfig template = new PipelineTemplateConfig(targetTemplate, StageConfigMother.custom(targetStage.toString(), "stage1-job-1"));

        PipelineConfig pipelineUsingTemplate = new PipelineConfig();
        pipelineUsingTemplate.setName("my-pipeline");
        pipelineUsingTemplate.setTemplateName(targetTemplate);

        PipelineConfig normalPipeline = PipelineConfigMother.createPipelineConfig("downest", "downest_stage_1", "downest_job_1", "downest_job_1a");

        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipeline("first", pipelineUsingTemplate);
        cruiseConfig.addPipeline("first", normalPipeline);

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, ALL_VIEWABLE).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitViewableDirectUpstreamsAndAncestorsReachableThroughHiddenIntermediates() {
        var targetPipeline = cis("downest");
        var targetStage = cis("downest_stage_3");

        // Hidden: upper (intermediate between viewable downer and viewable uppest)
        Set<CaseInsensitiveString> viewable = Set.of(targetPipeline, cis("downer"), cis("uppest"));

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, viewable::contains).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              },
              "downer": {
                "downer_stage_1": { "downer_job_1": {}, "downer_job_1a": {} },
                "downer_stage_2": { "downer_job_2": {} }
              },
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitViewableAncestorThroughMultipleConsecutiveHiddenIntermediates() {
        var targetPipeline = cis("downest");
        var targetStage = cis("downest_stage_3");
        // Hidden: upper, downer (two consecutive hidden hops between viewable downest and viewable uppest)
        Set<CaseInsensitiveString> viewable = Set.of(targetPipeline, cis("uppest"));

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, viewable::contains).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              },
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitOnlyViewablePipelineStagesUnderEveryReachablePathInTemplateAutoSuggest() {
        var targetTemplate = cis("template-1");
        var targetStage = cis("t1-stage-3");
        // Hidden: every pipeline except uppest (upper, downer, downest, downest_wmp, random_pipeline, randomOther_pipeline)
        Set<CaseInsensitiveString> viewable = Set.of(cis("uppest"));

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, viewable::contains).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} },
                "uppest_stage_3": { "uppest_job_3": {} }
              },
              "uppest/upper": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "": {
                "t1-stage-1": { "t1-job-1": {}, "t1-job-1a": {} },
                "t1-stage-2": { "t1-job-2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitOnlyLocalStagesWhenAllUpstreamPipelinesAreHidden() {
        var targetPipeline = cis("downest");
        CaseInsensitiveString targetStage = cis("downest_stage_3");
        // Hidden: every pipeline except the requested downest (so all of its upstreams downer/upper/uppest)
        PermissionResolver onlyDownest = targetPipeline::equals;

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, onlyDownest).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "downest": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              },
              "": {
                "downest_stage_1": { "downest_job_1": {}, "downest_job_1a": {} },
                "downest_stage_2": { "downest_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitViewableDirectUpstreamsAndDropHiddenSiblings() {
        var targetPipeline = cis("downest_wmp");
        var targetStage = cis("downest_wmp_stage_3");
        // Hidden: downer, upper, uppest (chain ancestors of downest_wmp) and randomOther_pipeline
        Set<CaseInsensitiveString> viewable = Set.of(targetPipeline, cis("random_pipeline"));

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetPipeline, targetStage, false, viewable::contains).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "downest_wmp": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} }
              },
              "random_pipeline": {
                "mingle": { "mingle_job": {} }
              },
              "": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitOnlyTemplateLocalStagesWhenNoUpstreamPipelinesAreViewable() {
        var targetTemplate = cis("template-1");
        var targetStage = cis("t1-stage-3");
        // Hidden: every pipeline
        PermissionResolver none = name -> false;

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, none).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "": {
                "t1-stage-1": { "t1-job-1": {}, "t1-job-1a": {} },
                "t1-stage-2": { "t1-job-2": {} }
              }
            }""");
    }

    @Test
    public void shouldEmitViewablePipelinesUnderAllReachablePathsIncludingViaHiddenIntermediatesInTemplateAutoSuggest() {
        var targetTemplate = cis("template-1");
        var targetStage = cis("t1-stage-3");
        // Hidden: upper, downer, downest (intermediates between template seeds and viewable uppest), random_pipeline, randomOther_pipeline
        Set<CaseInsensitiveString> viewable = Set.of(cis("downest_wmp"), cis("uppest"));

        var hierarchy = new FetchArtifactViewHelper(cruiseConfig, targetTemplate, targetStage, true, viewable::contains).autosuggestMap();

        assertThatJson(GSON.toJson(hierarchy)).isEqualTo("""
            {
              "uppest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} },
                "uppest_stage_3": { "uppest_job_3": {} }
              },
              "uppest/upper": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "uppest/upper/downer/downest_wmp": {
                "uppest_stage_1": { "uppest_job_1": { "a1": "docker.plugin.id" }, "uppest_job_1a": {} },
                "uppest_stage_2": { "uppest_job_2": {} }
              },
              "downest_wmp": {
                "downest_wmp_stage_1": { "downest_wmp_job_1": {}, "downest_wmp_job_1a": {} },
                "downest_wmp_stage_2": { "downest_wmp_job_2": {} },
                "downest_wmp_stage_3": { "downest_wmp_job_3": {} }
              },
              "": {
                "t1-stage-1": { "t1-job-1": {}, "t1-job-1a": {} },
                "t1-stage-2": { "t1-job-2": {} }
              }
            }""");
    }
}
