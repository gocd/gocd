##########################################################################
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
##########################################################################

class AnalyticsController < ApplicationController
  include AuthenticationHelper

  layout 'single_page_app', only: [:index]

  before_action :check_admin_user_and_403, only: [:index]
  before_action :check_pipeline_exists, only: [:show]
  before_action :check_permissions, only: [:show]

  def index
    @view_title = 'Analytics'
    @pipeline_list = pipeline_configs_service.getGroupsForUser(CaseInsensitiveString.str(current_user.getUsername())).map do |pipelines_config|
      pipelines_config.getPipelines().map(&:getName)
    end.flatten
  end

  def show
    render json: analytics_extension.getAnalytics(params[:plugin_id], params[:type], params[:id], request.query_parameters).toMap().to_h
  rescue => e
    render_plugin_error e
  end

  private

  def check_permissions
    if is_request_for_pipeline_analytics?
      is_analytics_enabled_only_for_admins? ? check_admin_user_and_403 : check_user_can_see_pipeline
    elsif is_request_for_agent_analytics?
      is_analytics_enabled_only_for_admins? ? check_admin_user_and_403 : check_user_and_403
    else
      check_admin_user_and_403
    end
  end

  def render_plugin_error e
    log_java_error(e)
    render :text => "Error generating analytics from plugin - #{params[:plugin_id]}", status: 500
  end

  def log_java_error(e)
    cause = e.getMessage()
    stack_trace = com.google.common.base.Throwables.getStackTraceAsString(e)

    Rails.logger.error "#{cause}:\n\n#{stack_trace}"
  end

  def check_pipeline_exists
    return unless is_request_for_pipeline_analytics?

    pipeline_config = pipeline_config_service.getPipelineConfig(params[:pipeline_name])
    render_message "Cannot generate analytics. Pipeline with name:'#{params[:pipeline_name]}' not found.", 404 if pipeline_config.nil?
  end

  def is_analytics_enabled_only_for_admins?
    system_environment.enableAnalyticsOnlyForAdmins
  end

  def is_request_for_pipeline_analytics?
    params[:type] == 'pipeline'
  end

  def is_request_for_agent_analytics?
    params[:type] == 'agent'
  end
end
