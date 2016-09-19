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
  module Admin
    class PluggableScmsController < ApiV1::BaseController
      before_action :check_admin_user_or_group_admin_user_and_401
      before_action :check_for_stale_request, :check_for_scm_rename, only: [:update]


      def index
        scms = pluggable_scm_service.listAllScms
        render DEFAULT_FORMAT => ApiV1::Scms::PluggableScmsRepresenter.new(scms).to_hash(url_builder: self)
      end

      def show
        hash = get_scm_hash(params[:material_name])
        render DEFAULT_FORMAT => hash if stale?(etag: get_etag_for_scm_object(params[:material_name]))
      end

      def create
        result = HttpLocalizedOperationResult.new
        @scm = ApiV1::Scms::PluggableScmRepresenter.new(SCM.new).from_hash(params[:pluggable_scm])
        @scm.ensureIdExists
        pluggable_scm_service.createPluggableScmMaterial(current_user, @scm, result)
        handle_create_or_update_response(result, @scm)
      end

      def update
        result = HttpLocalizedOperationResult.new
        updated_scm = ApiV1::Scms::PluggableScmRepresenter.new(SCM.new).from_hash(params[:pluggable_scm])
        pluggable_scm_service.updatePluggableScmMaterial(current_user, updated_scm, result, get_etag_for_scm_object(params[:material_name]))
        handle_create_or_update_response(result, updated_scm)
      end

      private
      def handle_create_or_update_response(result, updated_scm)
        json = ApiV1::Scms::PluggableScmRepresenter.new(updated_scm).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [get_etag_for_scm_object(updated_scm.getName)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

      def check_for_stale_request
        if (request.env["HTTP_IF_MATCH"] != "\"#{Digest::MD5.hexdigest(get_etag_for_scm_object(params[:material_name]))}\"")
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'SCM', params[:material_name]))
          render_http_operation_result(result)
        end
      end

      def get_etag_for_scm_object(material_name)
        entity_hashing_service.md5ForEntity(find_scm(material_name))
      end

      def check_for_scm_rename
        unless params[:pluggable_scm][:name].downcase == params[:material_name].downcase
          render_message('Renaming of SCM material is not supported by this API.', :unprocessable_entity)
        end
      end

      def get_scm_hash(material_name)
        ApiV1::Scms::PluggableScmRepresenter.new(find_scm(material_name)).to_hash(url_builder: self)
      end

      def find_scm(material_name)
        scm = pluggable_scm_service.findPluggableScmMaterial(material_name)
        raise ApiV1::RecordNotFound unless scm
        scm
      end

    end
  end
end
