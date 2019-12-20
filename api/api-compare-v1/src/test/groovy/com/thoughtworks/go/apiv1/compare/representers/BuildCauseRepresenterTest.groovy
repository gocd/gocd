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

package com.thoughtworks.go.apiv1.compare.representers

import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class BuildCauseRepresenterTest {
  @Test
  void 'should serialize build cause to json'() {
    def materialRevisions = ModificationsMother.multipleModifications()
    def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

    def actualJson = toObjectString({ BuildCauseRepresenter.toJSON(it, buildCause) })

    def expectedJSON = [
      "trigger_message"   : "modified by committer <html />",
      "trigger_forced"    : false,
      "approver"          : "approver",
      "material_revisions": buildCause.getMaterialRevisions().collect { eachItem ->
        toObject({
          MaterialRevisionRepresenter.toJSON(it, eachItem)
        })
      }
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
