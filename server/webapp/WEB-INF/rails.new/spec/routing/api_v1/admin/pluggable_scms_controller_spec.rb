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

describe ApiV1::Admin::PluggableScmsController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to index action of pluggable_scms controller' do
        expect(:get => 'api/admin/scms').to route_to(action: 'index', controller: 'api_v1/admin/pluggable_scms')
      end
    end
    describe "without_header" do
      it 'should not route to index action of pluggable_scms controller without header' do
        expect(:get => 'api/admin/scms').to_not route_to(action: 'index', controller: 'api_v1/admin/pluggable_scms')
        expect(:get => 'api/admin/scms').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/scms')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of pluggable_scms controller for material name with dots' do
        expect(:get => 'api/admin/scms/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/pluggable_scms', material_name: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to show action of pluggable_scms controller without header' do
        expect(:get => 'api/admin/scms/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/pluggable_scms')
        expect(:get => 'api/admin/scms/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/scms/foo')
      end
    end
  end

  describe "create" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to create action of pluggable_scms controller' do
        expect(:post => 'api/admin/scms').to route_to(action: 'create', controller: 'api_v1/admin/pluggable_scms')
      end
    end
    describe "without_header" do
      it 'should not route to create action of pluggable_scms controller without header' do
        expect(:post => 'api/admin/scms').to_not route_to(action: 'create', controller: 'api_v1/admin/pluggable_scms')
        expect(:post => 'api/admin/scms').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/scms')
      end
    end
  end

  describe "update" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to update action of pluggable_scms controller for material name with dots' do
        expect(:put => 'api/admin/scms/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/pluggable_scms', material_name: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to update action of pluggable_scms controller without header' do
        expect(:put => 'api/admin/scms/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/pluggable_scms')
        expect(:put => 'api/admin/scms/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/scms/foo')
      end
    end
  end
end
