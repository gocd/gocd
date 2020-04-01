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
package com.thoughtworks.go.apiv2.compare.representers.material


import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.util.command.HgUrlArgument

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg

class HgMaterialRepresenterTest implements MaterialRepresenterTrait {

  static def existingMaterial() {
    return MaterialConfigsMother.hgMaterialConfigFull()
  }

  def existingMaterialWithErrors() {
    def hgConfig = hg(new HgUrlArgument(''), null, null, null, true, null, false, '/dest/', new CaseInsensitiveString('!nV@l!d'))
    def materialConfigs = new MaterialConfigs(hgConfig);
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))
    return materialConfigs.get(0)
  }

  def materialHash =
    [
      type      : 'hg',
      attributes: [
        url          : "http://user:pass@domain/path##branch",
        destination  : "dest-folder",
        filter       : [
          ignore: ['**/*.html', '**/foobar/']
        ],
        invert_filter: false,
        name         : "hg-material",
        auto_update  : true,
        display_type : "Mercurial",
        description  : "URL: http://user:******@domain/path##branch",
      ]
    ]

  def expectedMaterialHashWithErrors =
    [
      type      : "hg",
      attributes: [
        url          : "",
        destination  : "/dest/",
        filter       : null,
        invert_filter: false,
        name         : "!nV@l!d",
        auto_update  : true,
        display_type : "Mercurial",
        description  : "URL: ",
      ],
      errors    : [
        name       : ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
        destination: ["Dest folder '/dest/' is not valid. It must be a sub-directory of the working folder."],
        url        : ["URL cannot be blank"]
      ]
    ]
}
