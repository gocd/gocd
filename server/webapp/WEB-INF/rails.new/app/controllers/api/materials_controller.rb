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

class Api::MaterialsController < Api::ApiController

  def notify
    result = HttpLocalizedOperationResult.new
    material_update_service.notifyMaterialsForUpdate(current_user, params, result)
    render_localized_operation_result result
  end

  def list_configs
    material_configs = material_config_service.getMaterialConfigs(CaseInsensitiveString.str(current_user.getUsername()))
    material_api_models = []
    material_configs.each do |material_config|
      material_api_models << MaterialInstanceAPIModel.new(material_config)
    end
    render json: material_api_models
  end
end
