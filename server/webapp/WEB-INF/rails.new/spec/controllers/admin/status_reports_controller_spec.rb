##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe Admin::StatusReportsController do

  describe :routes do
    it 'should route to show' do
      {:get => '/admin/status_reports/pluginId'}.should route_to(:controller => 'admin/status_reports', :action => 'show', :plugin_id => 'pluginId')
      admin_status_report_path(:plugin_id => 'com.tw.myplugin').should == '/admin/status_reports/com.tw.myplugin'
    end
  end

  describe :security do
    before :each do
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil))
      @elastic_agent_extension.stub(:getStatusReport).and_return('report')
    end

    after :each do
      ElasticAgentMetadataStore.instance().clear()
    end

    it 'should be accessible only to admins' do
      login_as_admin

      expect(controller).to allow_action(:get, :show, :plugin_id => 'com.tw.myplugin')
    end

    it 'should be inaccessible by non-admins' do
      login_as_group_admin

      expect(controller).to disallow_action(:get, :show, :plugin_id => 'com.tw.myplugin')
    end

    it 'should be accessible by all if security is disabled' do
      disable_security
      login_as_anonymous

      expect(controller).to allow_action(:get, :show, :plugin_id => 'com.tw.myplugin')
    end
  end

  describe :show do
    before :each do
      login_as_admin
    end

    it 'should verify availability of plugin' do
      get :show, :plugin_id => 'invalid_plugin_id'

      expect(response.response_code).to eq(404)
    end

    it 'should return the status report for an available plugin' do
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, capabilities))
      controller.elastic_agent_extension.stub(:getStatusReport).with('com.tw.myplugin').and_return('status_report')

      get :show, :plugin_id => 'com.tw.myplugin'

      expect(response).to be_ok
      assigns[:status_report].should == 'status_report'
    end

    it 'should be not found if plugin does not support status_report endpoint' do
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, capabilities))
      controller.elastic_agent_extension.stub(:getStatusReport).with('com.tw.myplugin').and_raise(java.lang.UnsupportedOperationException.new)

      get :show, :plugin_id => 'com.tw.myplugin'

      expect(response.response_code).to eq(404)
    end
  end
end