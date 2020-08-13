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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials


import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother

class HgMaterialRepresenterTest implements MaterialRepresenterTrait {

  HgMaterialConfig existingMaterial() {
    return MaterialConfigsMother.hgMaterialConfigFull("http://user:pass@domain/path")
  }

  def materialHash() {
    [
      type       : 'hg',
      fingerprint: existingMaterial().fingerprint,
      attributes : [
        url        : "http://user:******@domain/path",
        name       : "hg-material",
        auto_update: true
      ]
    ]
  }
}
