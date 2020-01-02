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
package com.thoughtworks.go.apiv1.materialsearch.representers

import com.thoughtworks.go.domain.materials.MatchedRevision
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MatchedRevisionRepresenterTest {

  @Test
  void 'should create json'() {
    def commitDate = new Date(10000)
    def pipelineDate = new Date(20000)
    def matchedRevisions = [
      new MatchedRevision("abc", "9ea1cf", "9ea1cf0ae04be6088242a5b6275ed36eadfcf205", "username", commitDate, "commit message"),
      new MatchedRevision("abc", "pipeline/1/stage/1", pipelineDate, "label")
    ]

    def actualJson = toArrayString({ MatchedRevisionRepresenter.toJSON(it, matchedRevisions) })

    assertThatJson(actualJson).isEqualTo([
      [
        "revision": "9ea1cf0ae04be6088242a5b6275ed36eadfcf205",
        "user"    : "username",
        "date"    : jsonDate(commitDate),
        "comment" : "commit message"
      ],
      [
        "revision": "pipeline/1/stage/1",
        "date"    : jsonDate(pipelineDate),
        "comment" : "label"
      ]
    ])
  }

}
