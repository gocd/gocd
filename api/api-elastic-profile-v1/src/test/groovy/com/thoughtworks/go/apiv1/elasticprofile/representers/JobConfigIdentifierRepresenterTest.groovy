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

package com.thoughtworks.go.apiv1.elasticprofile.representers

import com.thoughtworks.go.domain.JobConfigIdentifier
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class JobConfigIdentifierRepresenterTest {

  @Test
  void 'should serialize to json'() {
    def jobConfigIdentifiers = Arrays.asList(
      new JobConfigIdentifier("P1", "S1", "J1"),
      new JobConfigIdentifier("P1", "S1", "J2"),
      new JobConfigIdentifier("P2", "S1", "J3")
    )

    def actualJsonRepresentation = JobConfigIdentifierRepresenter.toJSON(jobConfigIdentifiers)


    def expectedJson = [
      [pipeline_name: "P1", stage_name: "S1", job_name: "J1"],
      [pipeline_name: "P1", stage_name: "S1", job_name: "J2"],
      [pipeline_name: "P2", stage_name: "S1", job_name: "J3"]
    ]

    assertThatJson(actualJsonRepresentation).isEqualTo(expectedJson)
  }

  @Test
  void 'should serialize null list to empty json array'() {
    def actualJsonRepresentation = JobConfigIdentifierRepresenter.toJSON(null)

    assertThatJson(actualJsonRepresentation).isEqualTo([])
  }
}