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

describe AnalyticsController do

  describe 'routes' do
    it 'should resolve the path' do
      expect(:get => '/analytics/plugin_id/dashboard/test_metric').to route_to(controller: 'analytics', action: 'dashboard', plugin_id: 'plugin_id', metric: 'test_metric')
      expect(dashboard_analytics_path(plugin_id: 'test_plugin', metric: 'test_metric')).to eq('/analytics/test_plugin/dashboard/test_metric')

      expect(:get => '/analytics/plugin_id/pipelines/pipeline_name').to route_to(controller: 'analytics', action: 'pipeline', plugin_id: 'plugin_id', pipeline_name: 'pipeline_name')
      expect(pipeline_analytics_path(plugin_id: 'test_plugin', pipeline_name: 'test_pipeline')).to eq('/analytics/test_plugin/pipelines/test_pipeline')
    end
  end

  describe 'authorization' do
    before(:each) do
      login_as_admin
      allow_current_user_to_access_pipeline('pipeline_name')
    end

    it 'should only allow pipeline viewers to see pipeline analytics' do
      expect(controller).to allow_action(:get, :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name')

      allow_current_user_to_not_access_pipeline('pipeline_name')
      expect(controller).not_to allow_action(:get, :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name')
    end

    it 'should allow all users to view pipeline analytics if security is disabled' do
      allow_current_user_to_not_access_pipeline('pipeline_name')
      expect(controller).not_to allow_action(:get, :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name')

      disable_security
      expect(controller).to allow_action(:get, :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name')
    end
  end

  describe 'analytics dashboard' do
    before(:each) do
      login_as_admin
    end

    it 'should include the plugin ids in the SPA skeleton' do
      plugin_info_finder = instance_double('DefaultPluginInfoFinder')
      cap = instance_double('Capabilities')
      info = instance_double('AnalyticsPluginInfo')
      descriptor = com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor.new("com.tw.myplugin", nil, nil, nil, nil, false);

      allow(info).to receive(:getDescriptor).and_return(descriptor)
      allow(info).to receive(:getCapabilities).and_return(cap)
      allow(cap).to receive(:supportsDashboardAnalytics).and_return(true)
      allow(cap).to receive(:supportedAnalyticsDashboardMetrics).and_return(["foo"])

      allow(controller).to receive(:default_plugin_info_finder).and_return(plugin_info_finder)
      allow(plugin_info_finder).to receive(:allPluginInfos).with(PluginConstants.ANALYTICS_EXTENSION).and_return([info])

      get :index

      expect(response).to be_ok
      expect(controller.instance_variable_get(:@supported_dashboard_metrics)).to eq({"com.tw.myplugin" => ["foo"]})
    end

    it 'should render the analytics data for the dashboard' do
      analytics_extension = instance_double('AnalyticsExtension')
      analytics_data = com.thoughtworks.go.plugin.domain.analytics.AnalyticsData.new("dashboard_analytics", "/path/to/view")

      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)
      allow(analytics_extension).to receive(:getDashboardAnalytics).with('com.tw.myplugin', 'foo').and_return(analytics_data)

      get :dashboard, plugin_id: 'com.tw.myplugin', metric: 'foo'

      expect(response).to be_ok

      response_json = JSON.parse(response.body)
      expect(response_json['data']).to eq('dashboard_analytics')
      expect(response_json['view_path']).to eq('/path/to/view')
      expect(response.content_type).to eq('application/json')
    end
  end

  describe 'pipeline' do
    before(:each) do
      login_as_admin
      allow_current_user_to_access_pipeline('pipeline_name')
    end

    it 'should render analytics for a pipeline' do
      analytics_extension = instance_double('AnalyticsExtension')
      analytics_data = com.thoughtworks.go.plugin.domain.analytics.AnalyticsData.new("pipeline_analytics", "/path/to/view")

      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)
      allow(analytics_extension).to receive(:getPipelineAnalytics).with('com.tw.myplugin', 'pipeline_name').and_return(analytics_data)

      get :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name'

      expect(response).to be_ok

      response_json = JSON.parse(response.body)
      expect(response_json['data']).to eq('pipeline_analytics')
      expect(response_json['view_path']).to eq('/path/to/view')
      expect(response.content_type).to eq('application/json')
    end

    it 'should render error template on error' do
      analytics_extension = instance_double('AnalyticsExtension')

      allow(analytics_extension).to receive(:getPipelineAnalytics).with('com.tw.myplugin', 'pipeline_name').and_raise(java.lang.Exception.new)
      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)

      get :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name'

      expect(response.code).to eq('500')
      expect(response.body).to eq('Error generating analytics from plugin - com.tw.myplugin')
    end
  end
end
