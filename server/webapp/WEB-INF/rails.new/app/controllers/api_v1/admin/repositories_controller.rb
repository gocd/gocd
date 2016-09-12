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
    class RepositoriesController < ApiV1::BaseController
      before_action :check_admin_user_and_401
      before_action :load_package_repository, only: [:show, :destroy, :update]
      before_action :check_for_stale_request, only: [:update]

      def show
        json = ApiV1::Config::PackageRepositoryRepresenter.new(@package_repo_config).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: get_etag_for_package_repository)
      end

      def index
        render DEFAULT_FORMAT => ApiV1::Config::PackageRepositoriesRepresenter.new(package_repository_service.getPackageRepositories()).to_hash(url_builder: self)
      end

      def create
        result = HttpLocalizedOperationResult.new
        get_package_repository_from_request
        package_repository_service.createPackageRepository(@package_repo_from_request, current_user, result)
        handle_config_save_result(result, @package_repo_from_request.getId())
      end

      def update
        result = HttpLocalizedOperationResult.new
        get_package_repository_from_request
        package_repository_service.updatePackageRepository(@package_repo_config, @package_repo_from_request, current_user, get_etag_for_package_repository, result)
        handle_config_save_result(result, @package_repo_from_request.getId())
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        package_repository_service.deleteRepository(current_user, @package_repo_config, result)
        render_http_operation_result(result)
      end

      private
      def load_package_repository(repo_id=params[:repo_id])
        @package_repo_config = package_repository_service.getPackageRepository(repo_id)
        raise RecordNotFound if @package_repo_config.nil?
      end

      def get_package_repository_from_request
        @package_repo_from_request ||= PackageRepository.new.tap do |config|
          ApiV1::Config::PackageRepositoryRepresenter.new(config).from_hash(params[:repository])
        end
      end

      def handle_config_save_result(result, repo_id)
        if result.isSuccessful
          load_package_repository(repo_id)
          json = ApiV1::Config::PackageRepositoryRepresenter.new(@package_repo_config).to_hash(url_builder: self)
          response.etag = [get_etag_for_package_repository]
          render DEFAULT_FORMAT => json
        else
          json = ApiV1::Config::PackageRepositoryRepresenter.new(@package_repo_from_request).to_hash(url_builder: self)
          render_http_operation_result(result, {data: json})
        end
      end

      def get_etag_for_package_repository
        entity_hashing_service.md5ForEntity(@package_repo_config, @package_repo_config.getId)
      end

      def check_for_stale_request
        if request.env['HTTP_IF_MATCH'] != "\"#{Digest::MD5.hexdigest(get_etag_for_package_repository)}\""
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Package Repository', params[:repo_id]))
          render_http_operation_result(result)
        end
      end
    end
  end
end