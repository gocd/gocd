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

  before_action :check_user_can_see_pipeline

  def pipeline
    render :text => "<div> #{analytics_extension.getPipelineAnalytics(params[:plugin_id], params[:pipeline_name])} </div>"
  rescue => e
    render_plugin_error e
  end

  private
  def render_plugin_error e
    render :text => "Error generating analytics for pipeline - #{params[:pipeline_name]}", status: 500
  end
end