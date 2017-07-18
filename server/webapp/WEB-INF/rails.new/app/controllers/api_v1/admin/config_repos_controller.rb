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
    class ConfigReposController < ApiV1::BaseController
      before_action :check_admin_user_and_401
      before_action :load_config_repo, only: [:show, :destroy, :update]
      before_action :check_for_stale_request, only: [:update]

      def show
        json = ApiV1::Config::ConfigRepoRepresenter.new(@config_repo).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: etag_for(@config_repo))
      end

      def index
        render DEFAULT_FORMAT => ApiV1::Config::ConfigReposRepresenter.new(config_repo_service.getConfigRepos).to_hash(url_builder: self)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        config_repo_service.deleteConfigRepo(@config_repo.getId, current_user, result)
        render_http_operation_result(result)
      end

      def create
        result = HttpLocalizedOperationResult.new
        get_config_repo_from_request
        config_repo_service.createConfigRepo(@config_repo_from_request, current_user, result)
        handle_config_save_result(result, @config_repo_from_request.getId)
      end

      def update
        result = HttpLocalizedOperationResult.new
        get_config_repo_from_request
        config_repo_service.updateConfigRepo(@config_repo.getId, @config_repo_from_request, etag_for(@config_repo), current_user, result)
        handle_config_save_result(result, @config_repo_from_request.getId)
      end

      protected

      def load_config_repo(id=params[:id])
        @config_repo = config_repo_service.getConfigRepo(id)
        raise RecordNotFound unless @config_repo
      end

      def get_config_repo_from_request
        @config_repo_from_request ||= ConfigRepoConfig.new.tap do |config|
          ApiV1::Config::ConfigRepoRepresenter.new(config).from_hash(params[:config_repo])
        end
      end

      def handle_config_save_result(result, repo_id)
        if result.isSuccessful
          load_config_repo(repo_id)
          json = ApiV1::Config::ConfigRepoRepresenter.new(@config_repo).to_hash(url_builder: self)
          response.etag = [etag_for(@config_repo)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result)
        end
      end

      def stale_message
        LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'config repo', params[:id])
      end

      def etag_for_entity_in_config
        etag_for(@config_repo)
      end
    end
  end
end