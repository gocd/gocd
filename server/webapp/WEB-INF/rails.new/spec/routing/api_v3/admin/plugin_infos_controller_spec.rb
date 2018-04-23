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

describe ApiV3::Admin::PluginInfosController do
  include ApiHeaderSetupForRouting

  describe "index" do
    describe "with_header" do
      before(:each) do
        setup_header
      end
      it 'should route to the index action of plugin_infos controller' do
        expect(:get => 'api/admin/plugin_info').to route_to(action: 'index', controller: 'api_v3/admin/plugin_infos')
      end
    end

    describe "without_header" do
      it 'should not route to index action of plugin_infos controller without header' do
        expect(:get => 'api/admin/plugin_info').to_not route_to(action: 'index', controller: 'api_v3/admin/plugin_infos')
        expect(:get => 'api/admin/plugin_info').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_info')
      end
    end
  end

  describe "show" do
    describe "with_header" do
      before(:each) do
        setup_header
      end

      it 'should route to the show action of plugin_infos controller for alphanumeric plugin id' do
        expect(:get => 'api/admin/plugin_info/foo123bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo123bar')
      end

      it 'should route to the show action of plugin_infos controller for plugin id with hyphen' do
        expect(:get => 'api/admin/plugin_info/foo-123-bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo-123-bar')
      end

      it 'should route to the show action of plugin_infos controller for plugin id with underscore' do
        expect(:get => 'api/admin/plugin_info/foo_123_bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo_123_bar')
      end

      it 'should route to the show action of plugin_infos controller for plugin id with dots' do
        expect(:get => 'api/admin/plugin_info/foo.123.bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo.123.bar')
      end

      it 'should route to the show action of plugin_infos controller for capitalized plugin id' do
        expect(:get => 'api/admin/plugin_info/FOO').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'FOO')
      end
    end

    describe "without_header" do
      it 'should not route to show action of plugin_infos controller without header' do
        expect(:get => 'api/admin/plugin_info/abc').to_not route_to(action: 'show', controller: 'api_v3/admin/plugin_infos')
        expect(:get => 'api/admin/plugin_info/abc').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_info/abc')
      end
    end
  end
end