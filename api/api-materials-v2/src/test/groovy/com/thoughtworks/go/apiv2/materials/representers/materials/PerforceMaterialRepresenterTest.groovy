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
package com.thoughtworks.go.apiv2.materials.representers.materials


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

  P4MaterialConfig existingMaterialWithErrors() {
    def p4Config = p4('', '', '', false, '', new GoCipher(), new CaseInsensitiveString(''), true, null, false, '/dest/')
    def materialConfigs = new MaterialConfigs(p4Config)
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))
    return materialConfigs.first() as P4MaterialConfig
  }

  def materialHash() {
    [
      type       : 'p4',
      fingerprint: existingMaterial().fingerprint,
      attributes : [
        destination       : "dest-folder",
        filter            : [
          ignore: ['**/*.html', '**/foobar/']
        ],
        invert_filter     : false,
        port              : "host:9876",
        username          : "user",
        encrypted_password: new GoCipher().encrypt("password"),
        use_tickets       : true,
        view              : "view",
        name              : "p4-material",
        auto_update       : true
      ]
    ]
  }

  def expectedMaterialHashWithErrors() {
    [
      type       : "p4",
      fingerprint: existingMaterialWithErrors().fingerprint,
      attributes : [
        destination  : "/dest/",
        filter       : null,
        invert_filter: false,
        name         : "",
        auto_update  : true,
        port         : "",
        username     : "",
        use_tickets  : false,
        view         : ""
      ],
      errors     : [
        view       : ["P4 view cannot be empty."],
        destination: ["Dest folder '/dest/' is not valid. It must be a sub-directory of the working folder."],
        port       : ["P4 port cannot be empty."]
      ]
    ]
  }

}
