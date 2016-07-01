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
        render DEFAULT_FORMAT => hash if stale?(etag: JSON.generate(hash))
      end

      def create
        result = HttpLocalizedOperationResult.new
        @scm = ApiV1::Scms::PluggableScmRepresenter.new(SCM.new).from_hash(params)
        @scm.ensureIdExists
        pluggable_scm_service.createPluggableScmMaterial(current_user, @scm, result)

        json = ApiV1::Scms::PluggableScmRepresenter.new(@scm).to_hash(url_builder: self)
        result.isSuccessful ? (render DEFAULT_FORMAT => json) : (render_http_operation_result(result, {data: json}))
      end

      def update
        result = HttpLocalizedOperationResult.new
        @scm = ApiV1::Scms::PluggableScmRepresenter.new(SCM.new).from_hash(params)
        pluggable_scm_service.updatePluggableScmMaterial(current_user, @scm, result)

        json = ApiV1::Scms::PluggableScmRepresenter.new(@scm).to_hash(url_builder: self)
        result.isSuccessful ? (render DEFAULT_FORMAT => json) : (render_http_operation_result(result, {data: json}))
      end

      private
      def check_for_stale_request
        if (request.env["HTTP_IF_MATCH"] != "\"#{get_etag_for_material(params[:material_name])}\"")
          render_message("Someone has modified the global SCM '#{params[:material_name]}'. Please update your copy of the config with the changes.", :precondition_failed)
        end
      end

      def get_etag_for_material(material_name)
        hash = get_scm_hash(material_name)
        get_etag_for_scm_json(hash)
      end

      def get_etag_for_scm_json(hash)
        Digest::MD5.hexdigest(JSON.generate(hash))
      end

      def check_for_scm_rename
        unless params[:name].downcase == params[:material_name].downcase
          render_message('Renaming of SCM material is not supported by this API.', :unprocessable_entity)
        end
      end

      def get_scm_hash(material_name)
        scm = pluggable_scm_service.findPluggableScmMaterial(material_name)
        raise ApiV1::RecordNotFound unless scm
        ApiV1::Scms::PluggableScmRepresenter.new(scm).to_hash(url_builder: self)
      end

    end
  end
end
