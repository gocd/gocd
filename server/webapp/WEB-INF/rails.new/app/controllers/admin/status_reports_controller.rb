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

module Admin
  class StatusReportsController < ::ApplicationController
    include AuthenticationHelper

    layout 'application'
    before_action :check_admin_user_and_401
    before_action :load_plugin_info

    def show
      @view_title = "Plugin Status Report"
      @page_header = "Plugin Status Report"
      @status_report = elastic_agent_extension.getStatusReport(params[:plugin_id])
    end

    def load_plugin_info
      @plugin_info = ElasticAgentMetadataStore.instance().getPluginInfo(params[:plugin_id])
      render_error_template "Plugin with id: #{params[:plugin_id]} is not found.", 404 if @plugin_info.nil?
    end
  end
end