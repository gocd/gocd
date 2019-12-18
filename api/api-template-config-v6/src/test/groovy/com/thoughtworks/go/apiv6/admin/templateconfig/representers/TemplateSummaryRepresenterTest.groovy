/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv6.admin.templateconfig.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineEditabilityInfo
import com.thoughtworks.go.config.TemplateToPipelines
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class TemplateSummaryRepresenterTest {

  @Test
  void 'should render a template name and its associated pipelines in hal representation'() {
    def templates = new TemplateToPipelines(new CaseInsensitiveString("template-name"), true, true)
    templates.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline2"), false, true))
    templates.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline1"), true, true))

    def actualJson = toObjectString({ TemplateSummaryRepresenter.toJSON(it, templates) })

    assertThatJson(actualJson).isEqualTo(indexHash)
  }

  def indexHash =
  [
    _links: [
      self: [
        href: 'http://test.host/go/api/admin/templates/template-name'
      ],
      doc: [
        href: apiDocsUrl('#template-config')
      ],
      find: [
        href: 'http://test.host/go/api/admin/templates/:template_name'
      ]
    ],
    name: 'template-name',
    can_edit: true,
    can_administer: true,
    _embedded: [
      pipelines: [
        [
          _links: [
            self: [
              href: 'http://test.host/go/api/admin/pipelines/pipeline2'
            ],
            doc: [
              href: apiDocsUrl('#pipeline-config')
            ],
            find: [
              href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
            ]
          ],
          name: 'pipeline2',
          can_administer: false

        ],
        [
          _links: [
            self: [
              href: 'http://test.host/go/api/admin/pipelines/pipeline1'
            ],
            doc: [
              href: apiDocsUrl('#pipeline-config')
            ],
            find: [
              href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
            ]
          ],
          name: 'pipeline1',
          can_administer: true
        ]
      ]
    ]
  ]
}
