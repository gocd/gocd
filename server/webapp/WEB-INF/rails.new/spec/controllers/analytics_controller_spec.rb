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

describe AnalyticsController do

  describe 'routes' do
    it 'should resolve the path' do
      expect(:get => '/analytics/plugin_id/pipeline/metric_id').to route_to(controller: 'analytics', action: 'show', plugin_id: 'plugin_id', type: 'pipeline', id: 'metric_id')
      expect(show_analytics_path(plugin_id: 'test_plugin', plugin_id: 'plugin_id', type: 'pipeline', id: 'metric_id')).to eq('/analytics/plugin_id/pipeline/metric_id')
    end
  end

  describe 'authorization' do
    before(:each) do
      @system_environment = double('system_environment')
      pipeline_config_service = instance_double('pipeline_config_service')

      login_as_admin
      allow_current_user_to_access_pipeline('pipeline_name')
      allow(controller).to receive(:system_environment).and_return(@system_environment)

      allow(controller).to receive(:pipeline_config_service).and_return(pipeline_config_service)
      allow(pipeline_config_service).to receive(:getPipelineConfig).with('pipeline_name').and_return('pipeline_config')
    end

    it 'should only allow pipeline viewers to see analytics of type pipeline' do
      allow_current_user_to_access_pipeline('pipeline_name')
      allow(@system_environment).to receive(:enablePipelineAnalyticsOnlyForAdmins).and_return(false)
      expect(controller).to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name', type: 'pipeline', id: 'metric_id')

      allow_current_user_to_not_access_pipeline('pipeline_name')
      expect(controller).not_to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name', type: 'pipeline', id: 'metric_id')
    end

    it 'should allow all users to view analytics of type pipeline if security is disabled' do
      allow_current_user_to_not_access_pipeline('pipeline_name')
      allow(@system_environment).to receive(:enablePipelineAnalyticsOnlyForAdmins).and_return(false)
      expect(controller).not_to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name', type: 'pipeline', id: 'metric_id')

      disable_security
      expect(controller).to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name', type: 'pipeline', id: 'metric_id')
    end

    it 'should disallow non admin users to view the analytics tab' do
      login_as_user
      expect(controller).not_to allow_action(:get, :index)
    end

    it 'should allow admins to view dashboard analytics' do
      login_as_admin

      expect(controller).to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', type: 'dashboard', id: 'metric_id')
    end

    it 'should disallow non admin users to view dashboard analytics' do
      login_as_user

      expect(controller).not_to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', type: 'dashboard', id: 'metric_id')
    end

    it 'should disallow non-admins from viewing pipeline analytics if system environment is configured to disallow' do
      login_as_user

      allow(@system_environment).to receive(:enablePipelineAnalyticsOnlyForAdmins).and_return(true)
      allow_current_user_to_not_access_pipeline('pipeline_name')
      expect(controller).not_to allow_action(:get, :show, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name', type: 'pipeline', id: 'metric_id')
    end
  end

  describe 'index' do
    before(:each) do
      login_as_admin
    end

    it 'should include the plugin ids in the SPA skeleton' do
      plugin_info_finder = instance_double('DefaultPluginInfoFinder')
      allow(controller).to receive(:default_plugin_info_finder).and_return(plugin_info_finder)

      get :index
      expect(response).to be_ok
    end
  end

  describe 'show' do
    before(:each) do
      login_as_admin
      allow_current_user_to_access_pipeline('pipeline_name')
    end

    it 'should render analytics for a pipeline' do
      analytics_extension = instance_double('AnalyticsExtension')
      analytics_data = com.thoughtworks.go.plugin.domain.analytics.AnalyticsData.new('pipeline_analytics', '/path/to/view')

      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)
      allow(analytics_extension).to receive(:getAnalytics).with('com.tw.myplugin', 'analytics_type', 'metric_id', {pipeline_name: 'pipeline_name', duration: '30 days'}).and_return(analytics_data)

      get :show, plugin_id: 'com.tw.myplugin', type: 'analytics_type', id: 'metric_id', pipeline_name: 'pipeline_name', duration: '30 days'

      expect(response).to be_ok

      response_json = JSON.parse(response.body)
      expect(response_json['data']).to eq('pipeline_analytics')
      expect(response_json['view_path']).to eq('/path/to/view')
      expect(response.content_type).to eq('application/json')
    end

    it 'should return 404 while fetching analytics for a non-existent pipeline' do
      pipeline_config_service = instance_double('pipeline_config_service')

      allow(controller).to receive(:pipeline_config_service).and_return(pipeline_config_service)
      allow(pipeline_config_service).to receive(:getPipelineConfig).with('pipeline_name').and_return(nil)

      get :show, plugin_id: 'com.tw.myplugin', type: 'pipeline', id: 'metric_id', pipeline_name: 'pipeline_name', duration: '30 days'

      expect(response.code).to eq('404')
    end
  end

  describe 'error handling' do
    before(:each) do
      login_as_admin
      allow_current_user_to_access_pipeline('pipeline')
    end

    it 'should render error template on error' do
      analytics_extension = instance_double('AnalyticsExtension')
      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)

      allow(analytics_extension).to receive(:getAnalytics).and_raise(java.lang.Exception.new)

      get :show, plugin_id: 'com.tw.myplugin', type: 'analytics_type', id: 'metric_id', pipeline_name: 'pipeline_name', duration: '30 days'

      expect(response.code).to eq('500')
      expect(response.body).to eq('Error generating analytics from plugin - com.tw.myplugin')
    end
  end
end
