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

describe ApiV1::Admin::PluginSettingsController do
  include ApiHeaderSetupForRouting

  describe 'show' do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of plugin_settings controller for plugin id with dots' do
        expect(:get => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to show action of plugin_settings controller without header' do
        expect(:get => 'api/admin/plugin_settings/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/plugin_settings')
        expect(:get => 'api/admin/plugin_settings/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings/foo')
      end
    end
  end

  describe 'create' do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of plugin_settings controller for plugin id with dots' do
        expect(:post => 'api/admin/plugin_settings').to route_to(action: 'create', controller: 'api_v1/admin/plugin_settings')
      end
    end
    describe "without_header" do
      it 'should not route to show action of plugin_settings controller without header' do
        expect(:post => 'api/admin/plugin_settings').to_not route_to(action: 'create', controller: 'api_v1/admin/plugin_settings')
        expect(:post => 'api/admin/plugin_settings').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings')
      end
    end
  end

  describe 'update' do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to show action of plugin_settings controller for plugin id with dots' do
        expect(:put => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
      end
    end
    describe "without_header" do
      it 'should not route to show action of plugin_settings controller without header' do
        expect(:put => 'api/admin/plugin_settings/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/plugin_settings')
        expect(:put => 'api/admin/plugin_settings/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings/foo')
      end
    end
  end
end