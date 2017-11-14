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

require 'rails_helper'

describe ApiV1::Admin::MergedEnvironmentsController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of environments controller' do
        expect(:get => 'api/admin/environments/merged').to route_to(action: 'index', controller: 'api_v1/admin/merged_environments')
      end
    end
    describe "without_header" do
      it 'should not route to index action of environments controller without header' do
        expect(:get => 'api/admin/environments/merged').to_not route_to(action: 'index', controller: 'api_v1/admin/merged_environments')
        expect(:get => 'api/admin/environments/merged').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/merged')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of environments controller for alphanumeric environment name' do
        expect(:get => 'api/admin/environments/foo123/merged').to route_to(action: 'show', controller: 'api_v1/admin/merged_environments', environment_name: 'foo123')
      end

      it 'should route to show action of environments controller for environment name with dots' do
        expect(:get => 'api/admin/environments/foo.123/merged').to route_to(action: 'show', controller: 'api_v1/admin/merged_environments', environment_name: 'foo.123')
      end

      it 'should route to show action of environments controller for environment name with hyphen' do
        expect(:get => 'api/admin/environments/foo-123/merged').to route_to(action: 'show', controller: 'api_v1/admin/merged_environments', environment_name: 'foo-123')
      end

      it 'should route to show action of environments controller for environment name with underscore' do
        expect(:get => 'api/admin/environments/foo_123/merged').to route_to(action: 'show', controller: 'api_v1/admin/merged_environments', environment_name: 'foo_123')
      end

      it 'should route to show action of environments controller for capitalized environment name' do
        expect(:get => 'api/admin/environments/FOO/merged').to route_to(action: 'show', controller: 'api_v1/admin/merged_environments', environment_name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to show action of environments controller without header' do
        expect(:get => 'api/admin/environments/foo/merged').to_not route_to(action: 'show', controller: 'api_v1/admin/merged_environments')
        expect(:get => 'api/admin/environments/foo/merged').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo/merged')
      end
    end
  end
end
