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

    self.response.headers['Content-Type'] = 'text/plain; charset=UTF-8'
    render_localized_operation_result result
  end

  def list_configs
    material_configs = material_config_service.getMaterialConfigs(CaseInsensitiveString.str(current_user.getUsername()))
    material_api_models = material_configs.collect do |material_config|
      MaterialConfigAPIModel.new(material_config)
    end
    render json: material_api_models
  end

  def modifications
    fingerprint = params[:fingerprint]
    offset = params[:offset].to_i
    page_size = 10
    result = HttpOperationResult.new

    material_config = material_config_service.getMaterialConfig(CaseInsensitiveString.str(current_user.getUsername()), fingerprint, result)

    if result.canContinue()
      modifications_count = material_service.getTotalModificationsFor(material_config)

      pagination = Pagination.pageStartingAt(offset, modifications_count, page_size)

      modifications = material_service.getModificationsFor(material_config, pagination)

      material_history_api_model = MaterialHistoryAPIModel.new(pagination, modifications)
      render json: material_history_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end
end
