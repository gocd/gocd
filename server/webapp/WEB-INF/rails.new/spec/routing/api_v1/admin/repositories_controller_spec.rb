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

describe ApiV1::Admin::RepositoriesController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of environments controller' do
        expect(:get => 'api/admin/repositories').to route_to(action: 'index', controller: 'api_v1/admin/repositories')
      end
    end
    describe "without_header" do
      it 'should not route to index action of environments controller without header' do
        expect(:get => 'api/admin/repositories').to_not route_to(action: 'index', controller: 'api_v1/admin/repositories')
        expect(:get => 'api/admin/repositories').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/repositories')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of package repositories controller for specified package repository id' do
        expect(:get => 'api/admin/repositories/foo123').to route_to(action: 'show', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
      end

      it 'should route to show action of repositories controller for repo_id with dots' do
        expect(:get => 'api/admin/repositories/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/repositories', repo_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to show action of package repositories controller without header' do
        expect(:get => 'api/admin/repositories/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/repositories')
        expect(:get => 'api/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/repositories/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destroy action of package repository controller for specified package repository id' do
        expect(:delete => 'api/admin/repositories/foo123').to route_to(action: 'destroy', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
      end
      it 'should route to destroy action of repositories controller for repo_id with dots' do
        expect(:delete => 'api/admin/repositories/foo.bar').to route_to(action: 'destroy', controller: 'api_v1/admin/repositories', repo_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of package repositories controller without header' do
        expect(:delete => 'api_v1/admin/repositories/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/repositories')
        expect(:delete => 'api_v1/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api_v1/admin/repositories/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of package repositories controller' do
        expect(:post => 'api/admin/repositories/').to route_to(action: 'create', controller: 'api_v1/admin/repositories')
      end
    end
    describe "without_header" do
      it 'should not route to create action of environments controller without header' do
        expect(:post => 'api/admin/repositories').to_not route_to(action: 'create', controller: 'api_v1/admin/repositories')
        expect(:post => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      
      it 'should route to update action of repositories controller for specified package repository id' do
        expect(:put => 'api/admin/repositories/foo123').to route_to(action: 'update', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
      end
      it 'should route to update action of repositories controller for repo_id with dots' do
        expect(:put => 'api/admin/repositories/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/repositories', repo_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to put action of repositories controller without header' do
        expect(:put => 'api_v1/admin/repositories/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/repositories')
        expect(:put => 'api_v1/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api_v1/admin/repositories/foo')
      end
    end
  end
end
