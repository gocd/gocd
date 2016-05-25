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

require 'spec_helper'

describe ApiV1::Admin::PluginsController do
  before(:each) do
    @plugin_service = double('plugin_service')
    controller.stub('plugin_service').and_return(@plugin_service)
  end

  describe :security do
    describe :show do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :show, {:id => 'plugin_id'}).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe :index do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe :index do
    before(:each) do
      login_as_group_admin
    end

    it 'should list all plugins' do
      plugin = PluginViewModel.new('plugin_id', 'plugin_version', 'plugin_type')

      @plugin_service.should_receive(:plugins).with(nil).and_return([plugin])

      get_with_api_header :index

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin], ApiV1::PluginsRepresenter))
    end

    it 'should filter plugins by type' do
      plugin = PluginViewModel.new('plugin_id', 'plugin_version', 'plugin_type')

      @plugin_service.should_receive(:plugins).with('scm').and_return([plugin])

      get_with_api_header :index, type: 'scm'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin], ApiV1::PluginsRepresenter))
    end
  end

  describe :show do
    before(:each) do
      login_as_group_admin
    end

    it 'should fetch a plugin for the given id' do
      plugin = PluginViewModel.new('plugin_id', 'plugin_version', 'plugin_type')

      @plugin_service.should_receive(:plugin).with('plugin_id').and_return(plugin)

      get_with_api_header :show, id: 'plugin_id'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(plugin, ApiV1::PluginRepresenter))
    end

    it 'should return 404 in absence of plugin' do
      @plugin_service.should_receive(:plugin).with('plugin_id').and_return(nil)

      get_with_api_header :show, id: 'plugin_id'

      expect(response.code).to eq('404')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Either the resource you requested was not found, or you are not authorized to perform this action.')
    end
  end
end