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
package com.thoughtworks.go.apiv8.admin.templateconfig.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class TemplateConfigRepresenterTest {

  private PipelineTemplateConfig pipelineTemplateConfig;

  @BeforeEach
  void setUp() {
    pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString('some-template'), new StageConfig(new CaseInsensitiveString('stage'), new JobConfigs(new JobConfig(new CaseInsensitiveString('job')))))
  }

  @Test
  void 'should render a template with hal representation'() {
    def actualJson = toObjectString({ TemplateConfigRepresenter.toJSON(it, pipelineTemplateConfig) })

    assertThatJson(actualJson).isEqualTo(templateHash)
  }

  @Test
  void 'should deserialize given json to PipelineTemplateConfig object'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(templateHash)
    def deserializedObject = TemplateConfigRepresenter.fromJSON(jsonReader)

    assertEquals(pipelineTemplateConfig, deserializedObject)
  }

  def templateHash =
    [
      _links: [
        self: [
          href: 'http://test.host/go/api/admin/templates/some-template'
        ],
        doc : [
          href: apiDocsUrl('#template-config')
        ],
        find: [
          href: 'http://test.host/go/api/admin/templates/:template_name'
        ]
      ],
      name  : 'some-template',
      stages: [
        [
          name                   : "stage",
          fetch_materials        : true,
          clean_working_directory: false,
          never_cleanup_artifacts: false,
          approval               : [
            type                 : "success",
            allow_only_on_success: false,
            authorization        : [
              roles: [],
              users: []
            ]
          ],
          environment_variables  : [],
          jobs                   : [
            [
              name                 : "job",
              run_instance_count   : null,
              timeout              : null,
              environment_variables: [],
              resources            : [],
              tasks                : [],
              tabs                 : [],
              artifacts            : [],
            ]
          ]
        ]
      ]
    ]
}
