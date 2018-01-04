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

require 'rails_helper'

describe Admin::StatusReportsController do

  describe "routes" do
    it 'should route to show' do
      expect({:get => '/admin/status_reports/pluginId'}).to route_to(:controller => 'admin/status_reports', :action => 'show', :plugin_id => 'pluginId')
      expect(admin_status_report_path(:plugin_id => 'com.tw.myplugin')).to eq('/admin/status_reports/com.tw.myplugin')
    end
  end

  describe "security" do
    before :each do
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil))
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

  describe "show" do
    before :each do
      login_as_admin
    end

    it 'should verify availability of plugin' do
      get :show, :plugin_id => 'invalid_plugin_id'

      expect(response.response_code).to eq(404)
    end

    it 'should return the status report for an available plugin' do
      elastic_agent_extension = instance_double('com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension')
      allow(elastic_agent_extension).to receive(:getStatusReport).with('com.tw.myplugin', nil).and_return('status_report')

      allow(controller).to receive(:elastic_agent_extension).and_return(elastic_agent_extension)
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, capabilities))

      get :show, :plugin_id => 'com.tw.myplugin'

      expect(response).to be_ok
      expect(assigns[:status_report]).to eq('status_report')
    end

    it 'should return the status report with job relation provided the jon identifier for an available plugin' do
      job_id = '100'
      job_identifier = JobIdentifier.new('blah-pipeline', 1, 'blah-label', 'blah-stage', '2', 'do-nothing')
      job_identifier.setBuildId(job_id.to_i)

      job_instance = JobInstance.new
      job_instance.setIdentifier(job_identifier)

      allow(controller).to receive(:job_instance_service).and_return(job_instance_service = double('job instance service'))
      allow(job_instance_service).to receive(:buildById).and_return(job_instance)

      elastic_agent_extension = instance_double('com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension')
      allow(elastic_agent_extension).to receive(:getStatusReport).with('com.tw.myplugin', job_identifier).and_return('status_report')

      allow(controller).to receive(:elastic_agent_extension).and_return(elastic_agent_extension)
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, capabilities))

      get :show, :plugin_id => 'com.tw.myplugin', :job_id => job_id

      expect(response).to be_ok
      expect(assigns[:status_report]).to eq('status_report')
    end

    it 'should be not found if plugin does not support status_report endpoint' do
      elastic_agent_extension = instance_double('com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension')
      allow(elastic_agent_extension).to receive(:getStatusReport).with('com.tw.myplugin', nil).and_raise(java.lang.UnsupportedOperationException.new)

      allow(controller).to receive(:elastic_agent_extension).and_return(elastic_agent_extension)

      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new('com.tw.myplugin', nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, capabilities))

      get :show, :plugin_id => 'com.tw.myplugin'

      expect(response.response_code).to eq(404)
    end
  end
end