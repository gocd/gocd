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

describe ApiV2::Admin::EnvironmentsController do

  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to index action of environments controller' do
        expect(:get => 'api/admin/environments').to route_to(action: 'index', controller: 'api_v2/admin/environments')
      end
    end
    describe "without_header" do
      it 'should not route to index action of environments controller without header' do
        expect(:get => 'api/admin/environments').to_not route_to(action: 'index', controller: 'api_v2/admin/environments')
        expect(:get => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to show action of environments controller for alphanumeric environment name' do
        expect(:get => 'api/admin/environments/foo123').to route_to(action: 'show', controller: 'api_v2/admin/environments', name: 'foo123')
      end

      it 'should route to show action of environments controller for environment name with dots' do
        expect(:get => 'api/admin/environments/foo.123').to route_to(action: 'show', controller: 'api_v2/admin/environments', name: 'foo.123')
      end

      it 'should route to show action of environments controller for environment name with hyphen' do
        expect(:get => 'api/admin/environments/foo-123').to route_to(action: 'show', controller: 'api_v2/admin/environments', name: 'foo-123')
      end

      it 'should route to show action of environments controller for environment name with underscore' do
        expect(:get => 'api/admin/environments/foo_123').to route_to(action: 'show', controller: 'api_v2/admin/environments', name: 'foo_123')
      end

      it 'should route to show action of environments controller for capitalized environment name' do
        expect(:get => 'api/admin/environments/FOO').to route_to(action: 'show', controller: 'api_v2/admin/environments', name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to show action of environments controller without header' do
        expect(:get => 'api/admin/environments/foo').to_not route_to(action: 'show', controller: 'api_v2/admin/environments')
        expect(:get => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
      end
    end
  end

  describe "put" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to put action of environments controller for alphanumeric environment name' do
        expect(:put => 'api/admin/environments/foo123').to route_to(action: 'put', controller: 'api_v2/admin/environments', name: 'foo123')
      end

      it 'should route to put action of environments controller for environment name with dots' do
        expect(:put => 'api/admin/environments/foo.123').to route_to(action: 'put', controller: 'api_v2/admin/environments', name: 'foo.123')
      end

      it 'should route to put action of environments controller for environment name with hyphen' do
        expect(:put => 'api/admin/environments/foo-123').to route_to(action: 'put', controller: 'api_v2/admin/environments', name: 'foo-123')
      end

      it 'should route to put action of environments controller for environment name with underscore' do
        expect(:put => 'api/admin/environments/foo_123').to route_to(action: 'put', controller: 'api_v2/admin/environments', name: 'foo_123')
      end

      it 'should route to put action of environments controller for capitalized environment name' do
        expect(:put => 'api/admin/environments/FOO').to route_to(action: 'put', controller: 'api_v2/admin/environments', name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to put action of environments controller without header' do
        expect(:put => 'api/admin/environments/foo').to_not route_to(action: 'put', controller: 'api_v2/admin/environments')
        expect(:put => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
      end
    end
  end

  describe "patch" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to patch action of environments controller for alphanumeric environment name' do
        expect(:patch => 'api/admin/environments/foo123').to route_to(action: 'patch', controller: 'api_v2/admin/environments', name: 'foo123')
      end

      it 'should route to patch action of environments controller for environment name with dots' do
        expect(:patch => 'api/admin/environments/foo.123').to route_to(action: 'patch', controller: 'api_v2/admin/environments', name: 'foo.123')
      end

      it 'should route to patch action of environments controller for environment name with hyphen' do
        expect(:patch => 'api/admin/environments/foo-123').to route_to(action: 'patch', controller: 'api_v2/admin/environments', name: 'foo-123')
      end

      it 'should route to patch action of environments controller for environment name with underscore' do
        expect(:patch => 'api/admin/environments/foo_123').to route_to(action: 'patch', controller: 'api_v2/admin/environments', name: 'foo_123')
      end

      it 'should route to patch action of environments controller for capitalized environment name' do
        expect(:patch => 'api/admin/environments/FOO').to route_to(action: 'patch', controller: 'api_v2/admin/environments', name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to put action of environments controller without header' do
        expect(:patch => 'api/admin/environments/foo').to_not route_to(action: 'put', controller: 'api_v2/admin/environments')
        expect(:patch => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to destroy action of environments controller for alphanumeric environment name' do
        expect(:delete => 'api/admin/environments/foo123').to route_to(action: 'destroy', controller: 'api_v2/admin/environments', name: 'foo123')
      end

      it 'should route to destroy action of environments controller for environment name with dots' do
        expect(:delete => 'api/admin/environments/foo.123').to route_to(action: 'destroy', controller: 'api_v2/admin/environments', name: 'foo.123')
      end

      it 'should route to destroy action of environments controller for environment name with hyphen' do
        expect(:delete => 'api/admin/environments/foo-123').to route_to(action: 'destroy', controller: 'api_v2/admin/environments', name: 'foo-123')
      end

      it 'should route to destroy action of environments controller for environment name with underscore' do
        expect(:delete => 'api/admin/environments/foo_123').to route_to(action: 'destroy', controller: 'api_v2/admin/environments', name: 'foo_123')
      end

      it 'should route to destroy action of environments controller for capitalized environment name' do
        expect(:delete => 'api/admin/environments/FOO').to route_to(action: 'destroy', controller: 'api_v2/admin/environments', name: 'FOO')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of environments controller without header' do
        expect(:delete => 'api/admin/environments/foo').to_not route_to(action: 'destroy', controller: 'api_v2/admin/environments')
        expect(:delete => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to create action of environments controller' do
        expect(:post => 'api/admin/environments/').to route_to(action: 'create', controller: 'api_v2/admin/environments')
      end
    end
    describe "without_header" do
      it 'should not route to create action of environments controller without header' do
        expect(:post => 'api/admin/environments').to_not route_to(action: 'create', controller: 'api_v2/admin/environments')
        expect(:post => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
      end
    end
  end
end
