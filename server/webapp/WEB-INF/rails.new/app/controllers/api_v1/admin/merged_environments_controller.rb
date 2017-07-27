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
  module Admin
    class MergedEnvironmentsController < ApiV1::BaseController
      before_action :check_admin_user_and_401

      def index
        render DEFAULT_FORMAT => Admin::MergedEnvironments::MergedEnvironmentsConfigRepresenter.new(environment_config_service.getAllMergedEnvironments()).to_hash(url_builder: self)
      end

      def show
        environment_name = params[:environment_name]
        load_merged_environment(environment_name)
        json = ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: etag_for(environment_config_service.getEnvironmentForEdit(environment_name)))
      end

      protected

      def load_merged_environment(environment_name)
        result = HttpLocalizedOperationResult.new
        config_element = environment_config_service.getMergedEnvironmentforDisplay(environment_name, result)
        raise RecordNotFound if config_element.nil?
        @environment_config = config_element.getConfigElement()
      end
    end
  end
end
