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
package com.thoughtworks.go.apiv1.featuretoggles.representers

import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class FeatureTogglesRepresenterTest {

  @Test
  void 'should serialize to JSON'() {
    def toggle1 = new FeatureToggle("key1", "description1", true)
    def toggle2 = new FeatureToggle("key2", "description2", false)

    def json = toArrayString({ FeatureTogglesRepresenter.toJSON(it, new FeatureToggles(toggle1, toggle2)) })

    assertThatJson(json).isEqualTo([
      [
        key        : 'key1',
        has_changed: false,
        description: 'description1',
        value      : true
      ],
      [
        key        : 'key2',
        has_changed: false,
        description: 'description2',
        value      : false
      ]
    ])
  }
}
