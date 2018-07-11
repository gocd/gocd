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

module ApiV1
  module ProfilesControllerActions
    include JavaImports
    def index
      all_entities_from_config = service.listAll.values.to_a
      render BaseController::DEFAULT_FORMAT => all_entities_representer.new(all_entities_from_config.to_a).to_hash(url_builder: self)
    end

    def show
      entity_from_config = load_entity_from_config
      if stale?(etag: etag_for(entity_from_config))
        render BaseController::DEFAULT_FORMAT => entity_representer.new(entity_from_config).to_hash(url_builder: self)
      end
    end

    def update
      entity_from_config = load_entity_from_config
      entity_from_request = entity_representer.new(create_config_entity).from_hash(entity_json_from_request)

      result = HttpLocalizedOperationResult.new
      service.update(current_user, etag_for(entity_from_config), entity_from_request, result)
      handle_create_or_update_response(result, entity_from_request)
    end

    def create
      result = HttpLocalizedOperationResult.new
      entity_from_request = entity_representer.new(create_config_entity).from_hash(entity_json_from_request)
      service.create(current_user, entity_from_request, result)
      handle_create_or_update_response(result, entity_from_request)
    end

    def destroy
      result = HttpLocalizedOperationResult.new
      entity_from_request = load_entity_from_config
      service.delete(current_user, entity_from_request, result)
      render_http_operation_result(result)
    end
  end
end