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

class AnalyticsController < ApplicationController
  include AuthenticationHelper

  layout 'single_page_app', only: [:index]

  before_action :check_admin_user_and_401
  before_action :check_user_can_see_pipeline, only: [:pipeline]

  def index
    @view_title = 'Analytics'
    @supported_dashboard_metrics = default_plugin_info_finder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION).inject({})do |memo, plugin|
      key = plugin.getDescriptor().id()
      memo[key] = plugin.getCapabilities().supportedAnalyticsDashboardMetrics() if plugin.getCapabilities().supportsDashboardAnalytics()
      memo
    end
  end

  def dashboard
    render json: analytics_extension.getDashboardAnalytics(params[:plugin_id], params[:metric]).toMap().to_h
  rescue => e
    render_plugin_error e
  end

  def pipeline
    render :json => analytics_extension.getPipelineAnalytics(params[:plugin_id], params[:pipeline_name]).toMap().to_h
  rescue => e
    render_plugin_error e
  end

  private

  def render_plugin_error e
    message = (e.getMessage() =~ /^Interaction with plugin with id/) ? e.getCause().getMessage() : e.getMessage()
    Rails.logger.error("#{e.getMessage}\n#{com.google.common.base.Throwables.getStackTraceAsString(e)}")
    render :text => message, status: 500
  end
end
