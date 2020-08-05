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


import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.security.GoCipher

import static com.thoughtworks.go.helper.MaterialConfigsMother.p4

class PerforceMaterialRepresenterTest implements MaterialRepresenterTrait<P4MaterialConfig> {

  P4MaterialConfig existingMaterial() {
    MaterialConfigsMother.p4MaterialConfigFull()
  }

  def materialHash() {
    [
      type       : 'p4',
      fingerprint: existingMaterial().fingerprint,
      attributes : [
        port              : "host:9876",
        use_tickets       : true,
        view              : "view",
        name              : "p4-material",
        auto_update       : true
      ]
    ]
  }

}
