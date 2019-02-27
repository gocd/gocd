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

package com.thoughtworks.go.apiv4.admin.templateconfig.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineEditabilityInfo
import org.junit.jupiter.api.Test
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineSummaryRepresenterTest {

  @Test
  void "should render pipeline summary"() {
    def actualJson = toObjectString({ PipelineSummaryRepresenter.toJSON(it, new PipelineEditabilityInfo(new CaseInsensitiveString('pipeline1'), true, true))})

    def expected = [
      _links  : [
        doc : [
          href: "https://api.gocd.org/#pipeline-config"
        ],
        find: [
          href: "http://test.host/go/api/admin/pipelines/:pipeline_name"
        ],
        self: [
          href: "http://test.host/go/api/admin/pipelines/pipeline1"
        ]
      ],
      name    : "pipeline1",
      can_edit: true
    ]

    assertThatJson(actualJson).isEqualTo(expected)
  }
}
