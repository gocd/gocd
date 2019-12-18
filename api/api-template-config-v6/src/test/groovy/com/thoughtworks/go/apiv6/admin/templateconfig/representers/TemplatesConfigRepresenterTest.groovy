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
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class TemplatesConfigRepresenterTest {
  @Test
  void 'should render links'() {
    def template = new TemplateToPipelines(new CaseInsensitiveString("template-name"), true, true)
    template.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline1"), true, true))
    template.add(new PipelineEditabilityInfo(new CaseInsensitiveString("pipeline2"), false, true))

    def templates = new ArrayList<TemplateToPipelines>()
    templates.add(template)
    def actualJson = toObjectString({ TemplatesConfigRepresenter.toJSON(it, templates) })

    def expected = [
      _links: [
        self: [
          href: 'http://test.host/go/api/admin/templates'
        ],
        doc: [
          href: apiDocsUrl('#template-config')
        ]
      ],
      _embedded: [
        templates: [
          toObject({TemplateSummaryRepresenter.toJSON(it, templates.get(0))})
        ]
      ]
    ]
    assertThatJson(actualJson).isEqualTo(expected)
  }
}
