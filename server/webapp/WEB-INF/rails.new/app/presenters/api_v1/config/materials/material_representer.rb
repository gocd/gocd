##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

module ApiV1
  module Config
    module Materials
      class MaterialRepresenter < ApiV1::BaseRepresenter
        TYPE_TO_MATERIAL_MAP = {
          'git'        => GitMaterialConfig,
          'svn'        => SvnMaterialConfig,
          'hg'         => HgMaterialConfig,
          'p4'         => P4MaterialConfig,
          'tfs'        => TfsMaterialConfig,
          'dependency' => DependencyMaterialConfig,
          'package'    => PackageMaterialConfig,
          'plugin'     => PluggableSCMMaterialConfig
        }

        MATERIAL_TO_TYPE_MAP = TYPE_TO_MATERIAL_MAP.invert

        MATERIAL_TYPE_TO_REPRESENTER_MAP = {
          GitMaterialConfig          => GitMaterialRepresenter,
          SvnMaterialConfig          => SvnMaterialRepresenter,
          HgMaterialConfig           => HgMaterialRepresenter,
          P4MaterialConfig           => PerforceMaterialRepresenter,
          TfsMaterialConfig          => TfsMaterialRepresenter,
          DependencyMaterialConfig   => DependencyMaterialRepresenter,
          PackageMaterialConfig      => PackageMaterialRepresenter,
          PluggableSCMMaterialConfig => PluggableScmMaterialRepresenter
        }
        alias_method :material_config, :represented

        error_representer({
                            "materialName"      => "name",
                            "folder"            => "destination",
                            "autoUpdate"        => "auto_update",
                            "filterAsString"    => "filter",
                            "checkexternals"    => "check_externals",
                            "serverAndPort"     => "port",
                            "useTickets"        => "use_tickets",
                            "pipelineName"      => "pipeline",
                            "stageName"         => "stage",
                            "pipelineStageName" => "pipeline",
                            "packageId"         => "ref",
                            "scmId"             => "ref",
                            "password"          => "password",
                            "encryptedPassword" => "encrypted_password",
                          }
        )

        property :type, getter: lambda { |options| MATERIAL_TO_TYPE_MAP[self.class] }, skip_parse: true

        nested :attributes,
               decorator: lambda { |material_config, *|
                 MATERIAL_TYPE_TO_REPRESENTER_MAP[material_config.class]
               }

        class << self
          def get_material_type(type)
            TYPE_TO_MATERIAL_MAP[type] or (raise ApiV1::UnprocessableEntity, "Invalid material type '#{type}'. It has to be one of '#{TYPE_TO_MATERIAL_MAP.keys.join(' ')}'")
          end
        end

      end
    end
  end
end
