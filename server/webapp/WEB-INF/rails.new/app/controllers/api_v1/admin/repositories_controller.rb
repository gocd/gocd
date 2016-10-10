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
        render DEFAULT_FORMAT => json if stale?(etag: get_etag_for_package_repository(@package_repo_config))
      end

      def index
        render DEFAULT_FORMAT => ApiV1::Config::PackageRepositoriesRepresenter.new(package_repository_service.getPackageRepositories()).to_hash(url_builder: self)
      end

      def create
        result = HttpLocalizedOperationResult.new
        @package_repo_config = ApiV1::Config::PackageRepositoryRepresenter.new(PackageRepository.new).from_hash(params[:repository])
        package_repository_service.createPackageRepository(@package_repo_config, current_user, result)
        handle_config_save_result(result, @package_repo_config)
      end

      def update
        result = HttpLocalizedOperationResult.new
        updated_repository = ApiV1::Config::PackageRepositoryRepresenter.new(PackageRepository.new).from_hash(params[:repository])
        package_repository_service.updatePackageRepository(updated_repository, current_user, get_etag_for_package_repository(@package_repo_config), result, params[:repo_id])
        handle_config_save_result(result, updated_repository)
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

      def handle_config_save_result(result, updated_repository)
        json = ApiV1::Config::PackageRepositoryRepresenter.new(updated_repository).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [get_etag_for_package_repository(updated_repository)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

      def get_etag_for_package_repository(package_repository)
        entity_hashing_service.md5ForEntity(package_repository)
      end

      def check_for_stale_request
        if request.env['HTTP_IF_MATCH'] != %Q{"#{Digest::MD5.hexdigest(get_etag_for_package_repository(@package_repo_config))}"}
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Package Repository', params[:repo_id]))
          render_http_operation_result(result)
        end
      end
    end
  end
end