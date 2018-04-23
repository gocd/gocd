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

describe ApiV2::UsersController do

  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to index action of users controller' do
        expect(:get => 'api/users').to route_to(action: 'index', controller: 'api_v2/users')
      end
    end
    describe "without_header" do
      it 'should not route to index action of users controller without header' do
        expect(:get => 'api/users').to_not route_to(action: 'index', controller: 'api_v2/users')
        expect(:get => 'api/users').to route_to(controller: 'application', action: 'unresolved', url: 'api/users')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to show action of users controller for alphanumeric login name' do
        expect(:get => 'api/users/foo123').to route_to(action: 'show', controller: 'api_v2/users', login_name: 'foo123')
      end

      it 'should route to show action of users controller having dots in login name' do
        expect(:get => 'api/users/foo.bar').to route_to(action: 'show', controller: 'api_v2/users', login_name: 'foo.bar')
      end

      it 'should route to show action of users controller for capitalized login name' do
        expect(:get => 'api/users/Foo').to route_to(action: 'show', controller: 'api_v2/users', login_name: 'Foo')
      end

      it 'should not route to show action of users controller for invalid login name' do
        expect(:get => 'api/users/foo#%$').not_to be_routable
      end
    end
    describe "without_header" do
      it 'should not route to show action of users controller without header' do
        expect(:get => 'api/users/foo').to_not route_to(action: 'show', controller: 'api_v2/users')
        expect(:get => 'api/users/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/users/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to destroy action of users controller for alphanumeric login name' do
        expect(:delete => 'api/users/foo123').to route_to(action: 'destroy', controller: 'api_v2/users', login_name: 'foo123')
      end

      it 'should route to destroy action of users controller having dots in login name' do
        expect(:delete => 'api/users/foo.bar').to route_to(action: 'destroy', controller: 'api_v2/users', login_name: 'foo.bar')
      end

      it 'should route to destroy action of users controller for capitalized login name' do
        expect(:delete => 'api/users/Foo').to route_to(action: 'destroy', controller: 'api_v2/users', login_name: 'Foo')
      end

      it 'should not route to show action of users controller for invalid login name' do
        expect(:delete => 'api/users/foo#%$').not_to be_routable
      end
    end
    describe "without_header" do
      it 'should not route to show action of users controller without header' do
        expect(:delete => 'api/users/foo').to_not route_to(action: 'destroy', controller: 'api_v2/users')
        expect(:delete => 'api/users/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/users/foo')
      end
    end
  end

  describe 'delete' do
    describe 'with header' do
      before(:each) do
        setup_header
      end
      it 'should route to the bulk_delete action for users controller' do
        expect(:delete => '/api/users').to route_to(action: 'bulk_delete', controller: 'api_v2/users')
      end
    end

    describe 'without header' do

      it 'should not route to the bulk_delete action for users controller' do
        expect(:delete => '/api/users').to_not route_to(action: 'bulk_delete', controller: 'api_v2/users')
        expect(:delete => '/api/users').to route_to(controller: 'application', action: 'unresolved', url: 'api/users')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to update action of users controller for alphanumeric login name' do
        expect(:patch => 'api/users/foo123').to route_to(action: 'update', controller: 'api_v2/users', login_name: 'foo123')
      end

      it 'should route to update action of users controller having dots in login name' do
        expect(:patch => 'api/users/foo.bar').to route_to(action: 'update', controller: 'api_v2/users', login_name: 'foo.bar')
      end

      it 'should route to update action of users controller for capitalized login name' do
        expect(:patch => 'api/users/Foo').to route_to(action: 'update', controller: 'api_v2/users', login_name: 'Foo')
      end

      it 'should not route to show action of users controller for invalid login name' do
        expect(:patch => 'api/users/foo#%$').not_to be_routable
      end
    end
    describe "without_header" do
      it 'should not route to show action of users controller without header' do
        expect(:patch => 'api/users/foo').to_not route_to(action: 'update', controller: 'api_v2/users')
        expect(:patch => 'api/users/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/users/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to create action of users controller' do
        expect(:post => 'api/users').to route_to(action: 'create', controller: 'api_v2/users')
      end
    end
    describe "without_header" do
      it 'should not route to create action of users controller without header' do
        expect(:post => 'api/users').to_not route_to(action: 'create', controller: 'api_v2/users')
        expect(:post => 'api/users').to route_to(controller: 'application', action: 'unresolved', url: 'api/users')
      end
    end
  end
end
