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
package com.thoughtworks.go.apiv9.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv9.admin.shared.representers.materials.MaterialsRepresenter
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

trait MaterialRepresenterTrait {

  @Test
  void 'should render material with hal representation'() {
    def actualJson = toObjectString({ MaterialsRepresenter.toJSON(it, existingMaterial()) })

    assertThatJson(actualJson).isEqualTo(materialHash)
  }

  @Test
  void "should render errors"() {
    def actualJson = toObjectString({ MaterialsRepresenter.toJSON(it, existingMaterialWithErrors()) })

    assertThatJson(actualJson).isEqualTo(expectedMaterialHashWithErrors)
  }

  @Test
  void 'should convert hash to Material'() {
    def json = toObjectString({ MaterialsRepresenter.toJSON(it, existingMaterial()) })
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
    def newMaterial = MaterialsRepresenter.fromJSON(jsonReader, getOptions())

    assertEquals(existingMaterial().isAutoUpdate(), newMaterial.isAutoUpdate())
    assertEquals(existingMaterial().getName(), newMaterial.getName())
    assertEquals(existingMaterial(), newMaterial)
  }
}
