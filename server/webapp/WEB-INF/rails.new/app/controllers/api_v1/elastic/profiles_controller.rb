##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
  module Elastic
    class ProfilesController < ::ApiV1::BaseController
      before_filter :check_admin_user_or_group_admin_user_and_401
      before_action :check_for_stale_request, :check_for_attempted_rename, only: [:update]

      include ProfilesControllerActions

      protected

      def entity_json_from_request
        params[:profile]
      end

      def service
        elastic_profile_service
      end

      def all_entities_representer
        ProfilesRepresenter
      end

      def entity_representer
        ProfileRepresenter
      end

      def create_config_entity
        ElasticProfile.new
      end

      def load_entity_from_config
        elastic_profile_service.findProfile(params[:profile_id]) || (raise ApiV1::RecordNotFound)
      end

      def stale_message
        LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Elastic agent profile', params[:profile_id])
      end

      def etag_for_entity_in_config
        etag_for(load_entity_from_config)
      end

      def check_for_attempted_rename
        unless params[:profile].try(:[], :id).to_s == params[:profile_id].to_s
          render_message('Renaming of elastic agent profile IDs is not supported by this API.', :unprocessable_entity)
        end
      end

    end
  end
end
