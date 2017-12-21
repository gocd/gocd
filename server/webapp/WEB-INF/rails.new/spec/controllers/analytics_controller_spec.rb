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
      expect(:get => '/analytics/plugin_id/pipeline_name').to route_to(controller: 'analytics', action: 'pipeline', plugin_id: 'plugin_id', pipeline_name: 'pipeline_name')
      expect(pipeline_analytics_path(plugin_id: 'test_plugin', pipeline_name: 'test_pipeline')).to eq('/analytics/test_plugin/test_pipeline')
    end
  end

  describe 'authorization' do
    before(:each) do
      login_as_user
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

  describe 'pipeline' do
    it 'should render analytics for a pipeline' do
      analytics_extension = instance_double('AnalyticsExtension')
      analytics_data = instance_double('AnalyticsData')
      allow(analytics_data).to receive(:getData).and_return("pipeline_analytics")
      allow(analytics_data).to receive(:getViewPath).and_return("path/to/view")
      allow(analytics_data).to receive(:toMap).and_return({"data" => "pipeline_analytics", "viewPath" => "/path/to/view"})

      allow(analytics_extension).to receive(:getPipelineAnalytics).with('com.tw.myplugin', 'pipeline_name').and_return(analytics_data)
      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)

      get :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name'

      expect(response).to be_ok
      expect(response.body).to eq('{"data":"pipeline_analytics","viewPath":"/path/to/view"}')
      expect(response.content_type).to eq('application/json')
    end

    it 'should render error template on error' do
      analytics_extension = instance_double('AnalyticsExtension')

      allow(analytics_extension).to receive(:getPipelineAnalytics).with('com.tw.myplugin', 'pipeline_name').and_raise(java.lang.Exception.new)
      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)

      get :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name'

      expect(response.code).to eq('500')
      expect(response.body).to eq('Error generating analytics for pipeline - pipeline_name')
    end
  end
end
