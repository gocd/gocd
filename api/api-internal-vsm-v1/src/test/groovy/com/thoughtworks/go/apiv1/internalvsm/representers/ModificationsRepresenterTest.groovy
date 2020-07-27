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

package com.thoughtworks.go.apiv1.internalvsm.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.materials.Modifications
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationsRepresenterTest {
  @Test
  void 'render modification details'() {
    def modification = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()

    def actualJson = toArrayString({
      ModificationsRepresenter.toJSON(it, new CaseInsensitiveString("fingerprint"), new Modifications(modification))
    })

    def expectedJson = [
      [
        revision     : "rev_1",
        user         : "lgao",
        comment      : "Fixing the not checked in files",
        modified_time: "2 days ago",
        locator      : "/go/materials/value_stream_map/fingerprint/rev_1"
      ]]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
