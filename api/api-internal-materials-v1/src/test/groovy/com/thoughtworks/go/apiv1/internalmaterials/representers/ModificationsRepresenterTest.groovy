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

import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.domain.materials.Modifications
import com.thoughtworks.go.helper.ModificationsMother
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static java.util.Collections.emptyList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ModificationsRepresenterTest {

  @Test
  void 'should render empty json if modifications is null'() {
    def actualJson = toObjectString({ ModificationsRepresenter.toJSON(it, null, null, "", "") })

    def expectedJson = [
      modifications: []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render empty json if modifications is empty'() {
    def actualJson = toObjectString({ ModificationsRepresenter.toJSON(it, emptyList(), null, "", "") })

    def expectedJson = [
      modifications: []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not return any links if there is only one page to be rendered'() {
    def mod = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()
    mod.id = 1

    def actualJson = toObjectString({
      ModificationsRepresenter.toJSON(it, asList(mod), new PipelineRunIdInfo(1, 1), "fingerprint", "")
    })

    def expectedJson = [
      modifications: [
        toObject({ ModificationRepresenter.toJSON(it, mod) })
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Nested
  class Links {
    def mods = new Modifications()
    def modsJson = []

    @BeforeEach
    void setUp() {
      for (int i = 6; i >= 2; i--) {

        def mod = new Modification("user", "comment: " + i, "email", new DateTime().minusHours(i).toDate(), "revision: " + i)
        mod.id = i
        mods.add(mod)
        modsJson.add(toObject({ ModificationRepresenter.toJSON(it, mod) }))
      }
    }

    @Test
    void 'should render both next and previous links if records are present'() {
      def actualJson = toObjectString({
        ModificationsRepresenter.toJSON(it, mods, new PipelineRunIdInfo(10, 1), "fingerprint", "")
      })

      def expectedJson = [
        _links       : [
          previous: [
            href: "http://test.host/go/api/internal/materials/fingerprint/modifications?before=6"
          ],
          next    : [
            href: "http://test.host/go/api/internal/materials/fingerprint/modifications?after=2"
          ]
        ],
        modifications: modsJson
      ]
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'should return only next link if the latest instance id is present'() {
      def actualJson = toObjectString({
        ModificationsRepresenter.toJSON(it, mods, new PipelineRunIdInfo(6, 1), "fingerprint", "")
      })

      def expectedJson = [
        _links       : [
          next: [
            href: "http://test.host/go/api/internal/materials/fingerprint/modifications?after=2"
          ]
        ],
        modifications: modsJson
      ]
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'should return only previous link if the oldest instance id is present'() {
      def actualJson = toObjectString({
        ModificationsRepresenter.toJSON(it, mods, new PipelineRunIdInfo(10, 2), "fingerprint", "")
      })

      def expectedJson = [
        _links       : [
          previous: [
            href: "http://test.host/go/api/internal/materials/fingerprint/modifications?before=6"
          ]
        ],
        modifications: modsJson
      ]
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'should return link with pattern if present'() {
      def actualJson = toObjectString({
        ModificationsRepresenter.toJSON(it, mods, new PipelineRunIdInfo(10, 2), "fingerprint", "some pattern")
      })

      def expectedJson = [
        _links       : [
          previous: [
            href: "http://test.host/go/api/internal/materials/fingerprint/modifications?before=6&pattern=some+pattern"
          ]
        ],
        modifications: modsJson
      ]
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }
  }
}
