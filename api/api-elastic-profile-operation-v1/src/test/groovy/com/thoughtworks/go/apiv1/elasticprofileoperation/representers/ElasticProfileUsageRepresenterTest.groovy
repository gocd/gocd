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
package com.thoughtworks.go.apiv1.elasticprofileoperation.representers

import com.thoughtworks.go.domain.ElasticProfileUsage
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ElasticProfileUsageRepresenterTest {

  @Test
  void 'should serialize to json'() {
    def elasticProfileUsages = Arrays.asList(
      new ElasticProfileUsage("LinuxPR", "build", "compile", "linux-pr", "gocd"),
      new ElasticProfileUsage("LinuxPR", "build", "tests", "linux-pr", "gocd"),

      new ElasticProfileUsage("WindowsPR", "clean", "clean-dirs", null, "config_repo"),
      new ElasticProfileUsage("WindowsPR", "clean", "clean-artifacts", null, "config_repo")
    )

    def actualJsonRepresentation = ElasticProfileUsageRepresenter.toJSON(elasticProfileUsages)

    def expectedJson = [
      [pipeline_name: "LinuxPR", stage_name: "build", job_name: "compile", template_name: "linux-pr", "pipeline_config_origin": "gocd"],
      [pipeline_name: "LinuxPR", stage_name: "build", job_name: "tests", template_name: "linux-pr", "pipeline_config_origin": "gocd"],

      [pipeline_name: "WindowsPR", stage_name: "clean", job_name: "clean-dirs", "pipeline_config_origin": "config_repo"],
      [pipeline_name: "WindowsPR", stage_name: "clean", job_name: "clean-artifacts", "pipeline_config_origin": "config_repo"]
    ]

    assertThatJson(actualJsonRepresentation).isEqualTo(expectedJson)
  }

  @Test
  void 'should serialize null list to empty json array'() {
    def actualJsonRepresentation = ElasticProfileUsageRepresenter.toJSON(null)

    assertThatJson(actualJsonRepresentation).isEqualTo([])
  }

  @Test
  void 'should serialize empty list to empty json array'() {
    def actualJsonRepresentation = ElasticProfileUsageRepresenter.toJSON(new ArrayList<ElasticProfileUsage>())

    assertThatJson(actualJsonRepresentation).isEqualTo([])
  }
}