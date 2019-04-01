##########################GO-LICENSE-START################################
# Copyright 2018 ThoughtWorks, Inc.
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
  elastic_plugin_id = 'com.tw.myplugin'

  describe 'plugin status report' do
    describe 'routes' do
      it 'should route to index' do
        expect({:get => '/admin/status_reports/pluginId'}).to route_to(:controller => 'admin/status_reports', :action => 'plugin_status', :plugin_id => 'pluginId')
        expect(admin_status_report_path(:plugin_id => elastic_plugin_id)).to eq('/admin/status_reports/com.tw.myplugin')
      end
    end

    describe 'security' do
      before :each do
        pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
        ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, nil))
      end

      after :each do
        ElasticAgentMetadataStore.instance().clear()
      end

      it 'should be accessible only to admins' do
        login_as_admin

        expect(controller).to allow_action(:get, :plugin_status, params: {:plugin_id => elastic_plugin_id})
      end

      it 'should be inaccessible by non-admins' do
        login_as_group_admin

        expect(controller).to disallow_action(:get, :plugin_status, params: {:plugin_id => elastic_plugin_id})
                                .with(403, 'You are not authorized to perform this action.')
      end

      it 'should be accessible by all if security is disabled' do
        disable_security
        login_as_anonymous

        expect(controller).to allow_action(:get, :plugin_status, params: {:plugin_id => elastic_plugin_id})
      end
    end

    before :each do
      login_as_admin
    end

    it 'should verify availability of plugin' do
      get :plugin_status, params:{:plugin_id => 'invalid_plugin_id'}

      expect(response.response_code).to eq(404)
    end

    it 'should return the plugin status report for an available plugin' do
      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      allow(elastic_agent_plugin_service).to receive(:getPluginStatusReport).with(elastic_plugin_id).and_return('status_report')

      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :plugin_status, params:{:plugin_id => elastic_plugin_id}

      expect(response).to be_ok
      expect(assigns[:status_report]).to eq('status_report')
    end

    it 'should be not found if plugin does not support plugin status_report endpoint' do
      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      allow(elastic_agent_plugin_service).to receive(:getPluginStatusReport).with(elastic_plugin_id).and_raise(java.lang.UnsupportedOperationException.new)
      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)


      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(false)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :plugin_status, params:{:plugin_id => elastic_plugin_id}

      expect(response.response_code).to eq(404)
    end
  end

  describe 'agent status report using job id' do
    describe 'routes' do
      it 'should route to show' do
        expect({:get => '/admin/status_reports/pluginId/unassigned?job_id=jobId'}).to route_to(:controller => 'admin/status_reports', :action => 'agent_status', :plugin_id => 'pluginId', :elastic_agent_id => 'unassigned', :job_id => 'jobId')
        expect(admin_agent_status_report_path(:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned', :job_id => '100')).to eq('/admin/status_reports/com.tw.myplugin/unassigned?job_id=100')
      end
    end

    describe 'security' do
      before :each do
        pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
        ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, nil))
      end

      after :each do
        ElasticAgentMetadataStore.instance().clear()
      end

      it 'should be accessible only to admins' do
        login_as_admin

        expect(controller).to allow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned', :job_id => '100'})
      end

      it 'should be inaccessible by non-admins' do
        login_as_group_admin

        expect(controller).to disallow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned', :job_id => '100'})
                                .with(403, 'You are not authorized to perform this action.')
      end

      it 'should be accessible by all if security is disabled' do
        disable_security
        login_as_anonymous

        expect(controller).to allow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned', :job_id => '100'})
      end
    end

    before :each do
      login_as_admin
    end

    it 'should verify availability of plugin' do
      get :agent_status, params:{:plugin_id => 'invalid_plugin_id', :job_id => '100', :elastic_agent_id => 'unassigned'}

      expect(response.response_code).to eq(404)
    end

    it 'should be unprocessable entity when required parameters are not provided' do
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true, true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned'}

      expect(response.response_code).to eq(422)
    end

    it 'should return the agent status report for an available plugin' do
      job_instance = double()

      job_identifier = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "100")

      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      job_instance_service = instance_double('job-instance-service')

      allow(elastic_agent_plugin_service).to receive(:getAgentStatusReport).with(elastic_plugin_id, job_identifier, nil).and_return('status_report')
      allow(job_instance_service).to receive(:buildById).with(100).and_return(job_instance)

      allow(job_instance).to receive(:getIdentifier).and_return(job_identifier)

      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)
      allow(controller).to receive(:job_instance_service).and_return(job_instance_service)

      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(false, true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :job_id => '100', :elastic_agent_id => 'unassigned'}

      expect(response).to be_ok
      expect(assigns[:agent_status_report]).to eq('status_report')
    end


    it 'should return the agent status report even if the job is not yet assigned to an agent' do
      job_instance = double()
      job_identifier = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "100")

      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      job_instance_service = instance_double('job-instance-service')

      allow(elastic_agent_plugin_service).to receive(:getAgentStatusReport).with(elastic_plugin_id, job_identifier, nil).and_return('status_report')
      allow(job_instance_service).to receive(:buildById).with(100).and_return(job_instance)

      allow(job_instance).to receive(:getIdentifier).and_return(job_identifier)
      allow(job_instance).to receive(:isAssignedToAgent).and_return(false)

      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)
      allow(controller).to receive(:job_instance_service).and_return(job_instance_service)

      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(false, true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :job_id => '100', :elastic_agent_id => 'unassigned'}

      expect(response).to be_ok
      expect(assigns[:agent_status_report]).to eq('status_report')
    end

    it 'should be not found if plugin does not support agent_status endpoint' do
      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      allow(elastic_agent_plugin_service).to receive(:getAgentStatusReport).with(elastic_plugin_id, '100').and_raise(java.lang.UnsupportedOperationException.new)

      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)

      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(false)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :job_id => '100', :elastic_agent_id => 'unassigned'}

      expect(response.response_code).to eq(404)
    end
  end

  describe 'agent status report using elastic agent id' do
    describe 'routes' do
      it 'should route to show' do
        expect({:get => '/admin/status_reports/pluginId/elasticAgentId'}).to route_to(:controller => 'admin/status_reports', :action => 'agent_status', :plugin_id => 'pluginId', :elastic_agent_id => 'elasticAgentId')
        expect(admin_agent_status_report_path(:plugin_id => elastic_plugin_id, :elastic_agent_id => 'elasticAgentId')).to eq('/admin/status_reports/com.tw.myplugin/elasticAgentId')
      end
    end

    describe 'security' do
      before :each do
        pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
        ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, nil))
      end

      after :each do
        ElasticAgentMetadataStore.instance().clear()
      end

      it 'should be accessible only to admins' do
        login_as_admin

        expect(controller).to allow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'agent1'})
      end

      it 'should be inaccessible by non-admins' do
        login_as_group_admin

        expect(controller).to disallow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'agent1'})
                                .with(403, 'You are not authorized to perform this action.')
      end

      it 'should be accessible by all if security is disabled' do
        disable_security
        login_as_anonymous

        expect(controller).to allow_action(:get, :agent_status, params: {:plugin_id => elastic_plugin_id, :elastic_agent_id => 'agent1'})
      end
    end

    before :each do
      login_as_admin
    end

    it 'should verify availability of plugin' do
      get :agent_status, params:{:plugin_id => 'invalid_plugin_id', :elastic_agent_id => 'agent1'}

      expect(response.response_code).to eq(404)
    end

    it 'should be unprocessable entity when required parameters are not provided' do
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :elastic_agent_id => 'unassigned'}

      expect(response.response_code).to eq(422)
    end

    it 'should return the status report for an available plugin' do
      elastic_agent_id = 'elastic_agent_1'

      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      allow(elastic_agent_plugin_service).to receive(:getAgentStatusReport).with(elastic_plugin_id, nil, elastic_agent_id).and_return('status_report')
      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)

      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)
      pluginDescriptor = GoPluginDescriptor.new(elastic_plugin_id, nil, nil, nil, nil, nil)
      ElasticAgentMetadataStore.instance().setPluginInfo(com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(pluginDescriptor, nil, nil, nil, nil, capabilities))

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :elastic_agent_id => elastic_agent_id}

      expect(response).to be_ok
      expect(assigns[:agent_status_report]).to eq('status_report')
    end

    it 'should be not found if plugin does not support status_report endpoint' do
      elastic_agent_id = 'elastic_agent_1'

      elastic_agent_plugin_service = instance_double('com.thoughtworks.go.server.service.ElasticAgentPluginService')
      allow(elastic_agent_plugin_service).to receive(:getAgentStatusReport).with(elastic_plugin_id, nil, elastic_agent_id).and_raise(java.lang.UnsupportedOperationException.new)
      allow(controller).to receive(:elastic_agent_plugin_service).and_return(elastic_agent_plugin_service)

      get :agent_status, params:{:plugin_id => elastic_plugin_id, :elastic_agent_id => elastic_agent_id}

      expect(response.response_code).to eq(404)
    end
  end
end
