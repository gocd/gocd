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

describe ApiV1::Admin::ConfigReposController do
  include ApiHeaderSetupForRouting

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of config repo controller for specified repo id' do
        expect(:get => 'api/admin/config_repos/foo').to route_to(action: 'show', controller: 'api_v1/admin/config_repos', id: 'foo')
      end
      it 'should route to show action of config repo controller for config id with dots' do
        expect(:get => 'api/admin/config_repos/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
      end
    end

    describe "without_header" do
      it 'should not route to show action of config repo controller without header' do
        expect(:get => 'api/admin/config_repos/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/config_repos')
        expect(:get => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
      end
    end
  end

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of config repos controller' do
        expect(:get => 'api/admin/config_repos').to route_to(action: 'index', controller: 'api_v1/admin/config_repos')
      end
    end

    describe "without_header" do

      it 'should not route to index action of config repos controller without header' do
        expect(:get => 'api/admin/config_repos').to_not route_to(action: 'index', controller: 'api_v1/admin/config_repos')
        expect(:get => 'api/admin/config_repos').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destroy action of config repo controller for specified repo id' do
        expect(:delete => 'api/admin/config_repos/foo').to route_to(action: 'destroy', controller: 'api_v1/admin/config_repos', id: 'foo')
      end

      it 'should route to delete action of config repo controller for id with dots' do
        expect(:delete => 'api/admin/config_repos/foo.bar').to route_to(action: 'destroy', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of config repos controller without header' do
        expect(:delete => 'api/admin/config_repos/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/config_repos')
        expect(:delete => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of config repo controller' do
        expect(:post => 'api/admin/config_repos').to route_to(action: 'create', controller: 'api_v1/admin/config_repos')
      end
    end
    describe "without_header" do
      it 'should not route to create action of config repo controller without header' do
        expect(:post => 'api/admin/config_repos').to_not route_to(action: 'create', controller: 'api_v1/admin/config_repos')
        expect(:post => 'api/admin/config_repos').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to update action of config repo controller for specified package id' do
        expect(:put => 'api/admin/config_repos/foo123').to route_to(action: 'update', controller: 'api_v1/admin/config_repos', id: 'foo123')
      end
      it 'should route to update action of config repo controller for package_id with dots' do
        expect(:put => 'api/admin/config_repos/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to update action of packages controller without header' do
        expect(:put => 'api/admin/config_repos/foo').to_not route_to(put: 'update', controller: 'api_v1/admin/config_repos')
        expect(:put => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
      end
    end
  end
end
