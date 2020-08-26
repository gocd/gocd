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

package com.thoughtworks.go.apiv1.internalmaterials.representers

import com.thoughtworks.go.domain.materials.Modification
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.ModificationsMother.TWO_DAYS_AGO_CHECKIN
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationRepresenterTest {
  @Test
  void 'should render modification'() {
    def mod = new Modification("", "Fixing the not checked in files", "foo@bar.com", TWO_DAYS_AGO_CHECKIN, "rev_1")

    def actualJson = toObjectString({ ModificationRepresenter.toJSON(it, mod) })

    def expectedJson = [
      "username"     : "anonymous",
      "email_address": "foo@bar.com",
      "revision"     : mod.revision,
      "modified_time": jsonDate(mod.modifiedTime),
      "comment"      : "Fixing the not checked in files"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
