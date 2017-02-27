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

module ApiV2
  module Admin
    class EnvironmentsWithRemoteController < ApiV2::BaseController
      before_action :check_admin_user_and_401

      def show
        load_remote_environment
        json = ApiV2::Config::EnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: etag_for(environment_config_service.forEdit(params[:name])))
      end

      protected

      def load_remote_environment(environment_name = params[:name])
        result = HttpLocalizedOperationResult.new
        @environment_config = environment_config_service.forDisplay(environment_name, result).getConfigElement()
      rescue com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException
        raise ApiV2::RecordNotFound
      end
    end
  end
end
