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

describe ApiV1::Admin::PluginInfosController do
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

    it 'should list all plugin_infos' do
      plugin_info = PluginInfo.new('plugin_id', 'plugin_name','plugin_version', 'plugin_type', nil, nil)

      @plugin_service.should_receive(:pluginInfos).with(nil).and_return([plugin_info])

      get_with_api_header :index

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV1::Plugin::PluginInfosRepresenter))
    end

    it 'should filter plugin_infos by type' do
      plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', nil, nil)

      @plugin_service.should_receive(:pluginInfos).with('scm').and_return([plugin_info])

      get_with_api_header :index, type: 'scm'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV1::Plugin::PluginInfosRepresenter))
    end

    it 'should be a unprocessible entity for a invalid plugin type' do
      @plugin_service.should_receive(:pluginInfos).with('invalid_type').and_raise(InvalidPluginTypeException.new)

      get_with_api_header :index, type: 'invalid_type'

      expect(response.code).to eq('422')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Your request could not be processed. Invalid plugins type - `invalid_type` !')
    end
  end

  describe :show do
    before(:each) do
      login_as_group_admin
    end

    it 'should fetch a plugin_info for the given id' do
      plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', nil, nil)

      @plugin_service.should_receive(:pluginInfo).with('plugin_id').and_return(plugin_info)

      get_with_api_header :show, id: 'plugin_id'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(plugin_info, ApiV1::Plugin::PluginInfoRepresenter))
    end

    it 'should return 404 in absence of plugin_info' do
      @plugin_service.should_receive(:pluginInfo).with('plugin_id').and_return(nil)

      get_with_api_header :show, id: 'plugin_id'

      expect(response.code).to eq('404')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Either the resource you requested was not found, or you are not authorized to perform this action.')
    end
  end
end