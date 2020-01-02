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
package com.thoughtworks.go.apiv1.compare.representers

import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
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

    def actualJson = toObjectString({ ModificationRepresenter.toJSON(it, modification) })

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

    def actualJson = toObjectString({ ModificationRepresenter.toJSON(it, modification) })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
