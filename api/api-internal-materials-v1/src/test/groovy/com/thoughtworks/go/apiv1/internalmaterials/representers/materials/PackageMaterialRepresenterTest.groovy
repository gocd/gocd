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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials


import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PackageMaterialRepresenterTest {

  @Test
  void "should represent a package material"() {
    PackageMaterialConfig packageMaterialConfig = MaterialConfigsMother.packageMaterialConfig()
    def actualJson = toObjectString(MaterialsRepresenter.toJSON(packageMaterialConfig))

    assertThatJson(actualJson).isEqualTo([
      type       : 'package',
      fingerprint: packageMaterialConfig.fingerprint,
      attributes :
        [
          ref              : "p-id",
          auto_update      : true,
          package_name     : "package-name",
          package_repo_name: "repo-name"
        ]
    ])
  }

}
