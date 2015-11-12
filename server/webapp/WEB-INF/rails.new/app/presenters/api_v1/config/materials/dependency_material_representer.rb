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
      class DependencyMaterialRepresenter < ApiV1::BaseRepresenter
        alias_method :material_config, :represented

        property :pipeline_name, as: :pipeline,case_insensitive_string: true
        property :stage_name, as: :stage,case_insensitive_string: true
        property :name,case_insensitive_string: true
        property :auto_update
      end
    end
  end
end