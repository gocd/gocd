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

describe ApiV1::Admin::PackagesController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of packages controller' do
        expect(:get => 'api/admin/packages').to route_to(action: 'index', controller: 'api_v1/admin/packages')
      end
    end
    describe "without_header" do
      it 'should not route to index action of packages controller without header' do
        expect(:get => 'api/admin/packages').to_not route_to(action: 'index', controller: 'api_v1/admin/packages')
        expect(:get => 'api/admin/packages').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/packages')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of packages controller for specified package id' do
        expect(:get => 'api/admin/packages/foo').to route_to(action: 'show', controller: 'api_v1/admin/packages', package_id: 'foo')
      end
      it 'should route to show action of packages controller for package_id with dots' do
        expect(:get => 'api/admin/packages/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/packages', package_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to show action of packages controller without header' do
        expect(:get => 'api/admin/packages/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/packages')
        expect(:get => 'api/admin/packages/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/packages/foo')
      end
    end
  end

  describe "destroy" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to destroy action of packages controller for specified package id' do
        expect(:delete => 'api/admin/packages/foo').to route_to(action: 'destroy', controller: 'api_v1/admin/packages', package_id: 'foo')
      end

      it 'should route to delete action of packages controller for package_id with dots' do
        expect(:delete => 'api/admin/packages/foo.bar').to route_to(action: 'destroy', controller: 'api_v1/admin/packages', package_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to destroy action of packages controller without header' do
        expect(:delete => 'api/admin/packages/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/packages')
        expect(:delete => 'api/admin/packages/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/packages/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of packages controller' do
        expect(:post => 'api/admin/packages').to route_to(action: 'create', controller: 'api_v1/admin/packages')
      end
    end
    describe "without_header" do
      it 'should not route to create action of packages controller without header' do
        expect(:post => 'api/admin/packages').to_not route_to(action: 'create', controller: 'api_v1/admin/packages')
        expect(:post => 'api/admin/packages').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/packages')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to update action of package controller for specified package id' do
        expect(:put => 'api/admin/packages/foo123').to route_to(action: 'update', controller: 'api_v1/admin/packages', package_id: 'foo123')
      end
      it 'should route to update action of packages controller for package_id with dots' do
        expect(:put => 'api/admin/packages/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/packages', package_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to update action of packages controller without header' do
        expect(:put => 'api/admin/packages/foo').to_not route_to(put: 'update', controller: 'api_v1/admin/packages')
        expect(:put => 'api/admin/packages/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/packages/foo')
      end
    end
  end
end
