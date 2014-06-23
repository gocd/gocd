##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

module Admin::Materials
  class DependencyController < ::Admin::MaterialsController
    include ::Admin::DependencyMaterialAutoSuggestions

    private
    def load_other_form_objects(cruise_config)
      assert_load :pipeline_stages_json, pipeline_stages_json(cruise_config, current_user, security_service, params)
    end

    def load_new_material(cruise_config)
      assert_load :material, DependencyMaterialConfig.new(CaseInsensitiveString.new(""), CaseInsensitiveString.new(""))
    end
  end
end
