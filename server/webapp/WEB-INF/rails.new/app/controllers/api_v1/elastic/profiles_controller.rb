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

      def index
        profiles = elastic_profile_service.allProfiles.values.to_a
        render DEFAULT_FORMAT => ProfilesRepresenter.new(profiles.to_a).to_hash(url_builder: self)
      end

      def show
        profile = load_profile
        if stale?(etag: etag_for(profile))
          render DEFAULT_FORMAT => ProfileRepresenter.new(profile).to_hash(url_builder: self)
        end
      end

      def update
        profile = load_profile
        profile_from_request = ProfileRepresenter.new(ElasticProfile.new).from_hash(params[:profile])

        result = HttpLocalizedOperationResult.new
        elastic_profile_service.update(current_user, etag_for(profile), profile_from_request, result)
        handle_create_or_update_response(result, profile_from_request)
      end

      def create
        result = HttpLocalizedOperationResult.new
        profile = ProfileRepresenter.new(ElasticProfile.new).from_hash(params[:profile])
        elastic_profile_service.create(current_user, profile, result)
        handle_create_or_update_response(result, profile)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        elastic_profile_service.delete(current_user, load_profile, result)
        render_http_operation_result(result)
      end

      private

      def load_profile
        elastic_profile_service.findProfile(params[:profile_id]) || (raise ApiV1::RecordNotFound)
      end

      def check_for_stale_request
        if request.env['HTTP_IF_MATCH'] != %Q{"#{Digest::MD5.hexdigest(etag_for(load_profile))}"}
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Elastic agent profile', params[:profile_id]))
          render_http_operation_result(result)
        end
      end

      def etag_for(profile)
        entity_hashing_service.md5ForEntity(profile)
      end

      def handle_create_or_update_response(result, updated_profile)
        json = ProfileRepresenter.new(updated_profile).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [etag_for(updated_profile)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

      def check_for_attempted_rename
        unless params[:profile].try(:[], :id).to_s == params[:profile_id].to_s
          render_message('Renaming of profile IDs is not supported by this API.', :unprocessable_entity)
        end
      end

    end
  end
end
