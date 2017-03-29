##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

module ApiV4
  module Admin
    module Pipelines
      module Materials
        class PluggableScmMaterialRepresenter < BaseRepresenter
          alias_method :material_config, :represented

          property :scmId, as: :ref, setter: lambda { |value, options|
            scm = options[:go_config].getSCMs().find(value)
            self.setSCMConfig(scm)
            self.setScmId(value)
          }

          property :filter,
                   decorator: FilterRepresenter,
                   class: com.thoughtworks.go.config.materials.Filter,
                   skip_parse: SkipParseOnBlank
          property :folder, as: :destination, skip_parse: SkipParseOnBlank
        end
      end
    end
  end
end