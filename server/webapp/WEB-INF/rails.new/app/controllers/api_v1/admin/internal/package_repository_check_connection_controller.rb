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
    module Internal
      class PackageRepositoryCheckConnectionController < ::ApiV1::BaseController
        before_action :check_admin_user_or_group_admin_user_and_401

        def repository_check_connection
          package_repository = PackageRepository.new
          result = HttpLocalizedOperationResult.new
          ApiV1::Config::Materials::CheckConnectionRepresenter.new(package_repository).from_hash(params[:package_repository_check_connection])
          package_repository_service.checkConnection(package_repository, result)
          render_http_operation_result(result)
        end

        def package_check_connection
          package_definition = PackageDefinition.new
          repository = package_repository_service.getPackageRepository(params[:repo_id])
          raise RecordNotFound if repository.blank?

          package_definition.setRepository(repository)
          ApiV1::Config::Materials::CheckConnectionRepresenter.new(package_definition).from_hash(params[:package_repository_check_connection])
          result = HttpLocalizedOperationResult.new
          package_definition_service.checkConnection(package_definition, result)
          render_http_operation_result(result)
        end

      end
    end
  end
end