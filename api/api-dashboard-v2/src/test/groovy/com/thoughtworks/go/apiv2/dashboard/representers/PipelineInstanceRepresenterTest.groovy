/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.dashboard.representers

import com.thoughtworks.go.apiv2.dashboard.PipelineModelMother
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class PipelineInstanceRepresenterTest {

  @Test
  void 'renders pipeline instance for pipeline that has never been executed with hal representation'() {
    def pipelineInstance = PipelineModelMother.pipeline_instance_model_empty("p1", "s1")

    def json = PipelineInstanceRepresenter.toJSON(pipelineInstance, new TestRequestContext())

    def expectedJson = [
      _links: [
        self       : [href: 'http://test.host/go/api/pipelines/p1/instance/0'],
        compare_url: [href: 'http://test.host/go/compare/p1/-1/with/0'],
        history_url: [href: 'http://test.host/go/api/pipelines/p1/history'],
        vsm_url    : [href: 'http://test.host/go/pipelines/value_stream_map/p1/0']
      ],
    ]

    assertThatJson(json._links).isEqualTo(expectedJson._links)
    assertThat(json._embedded.stages[0].name).isEqualTo('s1')
  }

  @Test
  void 'renders all pipeline instance with hal representation'() {
    def instance = PipelineModelMother.pipeline_instance_model([name  : "p1", label: "g1", counter: 5,
                                                                stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])

    def date = instance.getScheduledDate()

    def actualJson = PipelineInstanceRepresenter.toJSON(instance, new TestRequestContext())

    actualJson.remove("_links")
    actualJson.remove("_embedded")

    assertThatJson(actualJson).isEqualTo([
      label       : 'g1', counter: 5, scheduled_at: date,
      triggered_by: 'Triggered by Anonymous',
      build_cause : [approver          : 'anonymous',
                     is_forced         : true,
                     trigger_message   : "Forced by anonymous",
                     material_revisions: []]])
  }

  @Test
  void 'renders all pipeline instance with build_cause'() {
    def instance = PipelineModelMother.pipeline_instance_model([name  : "p1", label: "g1", counter: 5,
                                                                stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])
    def materialRevisions = ModificationsMother.createHgMaterialRevisions()
    def pipeline_dependency = ModificationsMother.dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date())
    materialRevisions.addRevision(pipeline_dependency)

    instance.setBuildCause(BuildCause.createWithModifications(materialRevisions, 'Anonymous'))

    def date = instance.getScheduledDate()

    def actualJson = PipelineInstanceRepresenter.toJSON(instance, new TestRequestContext())

    actualJson.remove("_links")
    actualJson.remove("_embedded")

    def expectedBuildCause = [approver          : 'Anonymous', is_forced: false, trigger_message: 'modified by user2',
                              material_revisions: [
                                [material_type: 'Mercurial',
                                 material_name: 'hg-url',
                                 changed      : false,
                                 modifications: [
                                   [
                                     _links       : [
                                       vsm: [
                                         href: 'http://test.host/go/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/9fdcf27f16eadc362733328dd481d8a2c29915e1'
                                       ]],
                                     user_name    : 'user2',
                                     email_address: 'email2',
                                     revision     : '9fdcf27f16eadc362733328dd481d8a2c29915e1',
                                     modified_time: materialRevisions.first().getModifications().first().getModifiedTime(),
                                     comment      : 'comment2'
                                   ],
                                   [
                                     _links       : [
                                       vsm: [
                                         href: 'http://test.host/go/materials/value_stream_map/4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515/eef77acd79809fc14ed82b79a312648d4a2801c6']
                                     ],
                                     user_name    : 'user1',
                                     email_address: 'email1',
                                     revision     : 'eef77acd79809fc14ed82b79a312648d4a2801c6',
                                     modified_time: materialRevisions.first().getModifications().last().getModifiedTime(),
                                     comment      : 'comment1'
                                   ]]
                                ],
                                [material_type: "Pipeline",
                                 material_name: "up1",
                                 changed      : false,
                                 modifications: [
                                   [_links        : [
                                     vsm              : [
                                       href: 'http://test.host/go/pipelines/value_stream_map/up1/1'
                                     ],
                                     stage_details_url: [
                                       href: 'http://test.host/go/pipelines/up1/1/first/1'
                                     ]
                                   ],
                                    revision      : 'up1/1/first/1',
                                    modified_time : materialRevisions.getMaterialRevision(1).getModifications().first().getModifiedTime(),
                                    pipeline_label: 'label'
                                   ]
                                 ]]
                              ]
    ]

    assertThatJson(actualJson).isEqualTo(
      [label: 'g1', counter: 5, scheduled_at: date, triggered_by: 'Triggered by Anonymous', build_cause: expectedBuildCause])

  }
}
