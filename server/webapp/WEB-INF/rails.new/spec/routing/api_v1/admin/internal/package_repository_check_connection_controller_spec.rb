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

require 'rails_helper'

describe ApiV1::Admin::Internal::PackageRepositoryCheckConnectionController do

  include ApiHeaderSetupForRouting

  describe 'repository_check_connection' do
    describe 'route' do
      describe 'with header' do
        before(:each) do
          setup_header
        end

        it 'should route to repository_check_connection action of the repository check connection controller' do
          expect(:post => 'api/admin/internal/repository_check_connection').to route_to(action: 'repository_check_connection', controller: 'api_v1/admin/internal/package_repository_check_connection')
        end
      end
      describe 'without_header' do
        it 'should not route to repository_check_connection action of repository check connection controller without header' do
          expect(:post => 'api/admin/internal/repository_check_connection').to_not route_to(action: 'repository_check_connection', controller: 'api_v1/admin/internal/package_repository_check_connection')
          expect(:post => 'api/admin/internal/repository_check_connection').to route_to(action: 'unresolved', controller: 'application', url: 'api/admin/internal/repository_check_connection')
        end
      end
    end
  end

  describe 'package_check_connection' do
    describe 'route' do
      describe 'with header' do
        before(:each) do
          setup_header
        end

        it 'should route to package_check_connection action of the repository check connection controller' do
          expect(:post => 'api/admin/internal/package_check_connection').to route_to(action: 'package_check_connection', controller: 'api_v1/admin/internal/package_repository_check_connection')
        end
      end
      describe 'without_header' do
        it 'should not route to package_check_connection action of repository check connection controller without header' do
          expect(:post => 'api/admin/internal/package_check_connection').to_not route_to(action: 'package_check_connection', controller: 'api_v1/admin/internal/package_repository_check_connection')
          expect(:post => 'api/admin/internal/package_check_connection').to route_to(action: 'unresolved', controller: 'application', url: 'api/admin/internal/package_check_connection')
        end
      end
    end
  end
end