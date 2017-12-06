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

      allow(analytics_extension).to receive(:getPipelineAnalytics).with('com.tw.myplugin', 'pipeline_name').and_return('pipeline_analytics')
      allow(controller).to receive(:analytics_extension).and_return(analytics_extension)

      get :pipeline, plugin_id: 'com.tw.myplugin', pipeline_name: 'pipeline_name'

      expect(response).to be_ok
      expect(response.body).to eq('pipeline_analytics')
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
