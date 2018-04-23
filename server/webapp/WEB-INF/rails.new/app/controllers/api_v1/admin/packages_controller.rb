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
    class PackagesController < ApiV1::BaseController
      before_action :check_admin_user_or_group_admin_user_and_401
      before_action :load_package, only: [:show, :destroy, :update]
      before_action :check_for_stale_request, only: [:update]
      before_action :check_for_repository, only: [:create, :update]

      def index
        render DEFAULT_FORMAT => ApiV1::Config::PackagesRepresenter.new(package_definition_service.getPackages).to_hash(url_builder: self)
      end

      def show
        json = ApiV1::Config::PackageRepresenter.new({package: @package}).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(strong_etag: etag_for(@package), template: false)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        package_definition_service.deletePackage(@package, current_user, result)
        render_http_operation_result(result)
      end

      def create
        result = HttpLocalizedOperationResult.new
        repository_id = params[:package][:package_repo][:id]
        new_package_repository_hash = ApiV1::Config::PackageRepresenter.new({package: PackageDefinition.new, repository: @repository}).from_hash(params[:package])
        new_package = new_package_repository_hash[:package]
        new_package.ensureIdExists
        package_definition_service.createPackage(new_package, repository_id, current_user, result)
        handle_config_save_result(result, new_package)
      end

      def update
        result = HttpLocalizedOperationResult.new
        updated_package_repo_hash = ApiV1::Config::PackageRepresenter.new({package: PackageDefinition.new, repository: @repository}).from_hash(params[:package])
        package_definition_service.updatePackage(@package.getId, updated_package_repo_hash[:package], etag_for(@package), current_user, result)
        handle_config_save_result(result, updated_package_repo_hash[:package])
      end

      protected

      def load_package(package_id = params[:package_id])
        @package = package_definition_service.find(package_id)
        raise RecordNotFound if @package.nil?
      end

      def check_for_repository(repository_id = params[:package][:package_repo][:id])
        @repository = package_repository_service.getPackageRepository(repository_id)
        if @repository.blank?
          result = HttpLocalizedOperationResult.new
          result.unprocessableEntity(LocalizedMessage::string('PACKAGE_REPOSITORY_NOT_FOUND', repository_id))
          render_http_operation_result(result)
        end
      end

      def handle_config_save_result(result, updated_package)
        json = ApiV1::Config::PackageRepresenter.new({package: updated_package}).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [etag_for(updated_package)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

      def stale_message
        LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'package', params[:package_id])
      end

      def etag_for_entity_in_config
        etag_for(@package)
      end
    end
  end
end
