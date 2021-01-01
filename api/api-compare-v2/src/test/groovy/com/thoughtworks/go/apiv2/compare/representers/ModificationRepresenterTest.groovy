/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv2.compare.representers

import com.thoughtworks.go.config.materials.Materials
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialsMother.dependencyMaterial
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationRepresenterTest {
  @Test
  void 'should render modified time if not null'() {
    def modification = ModificationsMother.aCheckIn("rev1", "file1")

    def expectedJSON = [
      "revision"     : "rev1",
      "modified_time": jsonDate(ModificationsMother.TODAY_CHECKIN),
      "user_name"    : "committer",
      "comment"      : "Added the README file",
      "email_address": "foo@bar.com"]

    def actualJson = toObjectString({ ModificationRepresenter.toMaterialJSON(it, modification) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should not render modified time if set to null'() {
    def modification = ModificationsMother.aCheckIn("rev1", "file1")
    modification.modifiedTime = null

    def expectedJSON = [
      "revision"     : "rev1",
      "user_name"    : "committer",
      "comment"      : "Added the README file",
      "email_address": "foo@bar.com"]

    def actualJson = toObjectString({ ModificationRepresenter.toMaterialJSON(it, modification) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should render dependency material revision'() {
    Materials materials = new Materials(dependencyMaterial())
    def modification = modifyOneFile(materials, "1").getMaterialRevision(0).getModification(0)

    def expectedJSON = [
      "revision"      : "pipeline-name/1/stage-name/1",
      "modified_time" : jsonDate(modification.modifiedTime),
      "pipeline_label": "pipeline-name-1.2.3"]

    def actualJson = toObjectString({ ModificationRepresenter.toDependencyJSON(it, modification) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should render modifications'() {
    Materials materials = new Materials(dependencyMaterial())
    def revision = modifyOneFile(materials, "1").getMaterialRevision(0)

    def expectedJSON = [
      [
        "modified_time" : jsonDate(revision.modifications.get(0).modifiedTime),
        "pipeline_label": "pipeline-name-1.2.3",
        "revision"      : "pipeline-name/1/stage-name/1"
      ]
    ]


    def actualJson = toArrayString({
      ModificationRepresenter.toJSONArray(it, revision.getModifications(), revision.isDependencyMaterialRevision())
    })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
