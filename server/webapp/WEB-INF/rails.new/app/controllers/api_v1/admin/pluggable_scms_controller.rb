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
      before_action :check_admin_user_and_401

      def index
        scms = pluggable_scm_service.listAllScms
        render json_hal_v1: ApiV1::Scms::PluggableScmsRepresenter.new(scms).to_hash(url_builder: self)
      end

      def show
        material_name = params[:material_name]
        scm = pluggable_scm_service.findPluggableScmMaterial(material_name)
        raise ApiV1::RecordNotFound if scm.nil?

        render json_hal_v1: ApiV1::Scms::PluggableScmRepresenter.new(scm).to_hash(url_builder: self)

      end

    end
  end
end