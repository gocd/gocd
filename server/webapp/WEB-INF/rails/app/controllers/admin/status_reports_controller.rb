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

module Admin
  class StatusReportsController < ::ApplicationController
    include AuthenticationHelper

    layout 'plugin'
    before_action :check_admin_user_and_403
    before_action :load_plugin_info

    def plugin_status
      @view_title = 'Plugin Status Report'
      @page_header = 'Plugin Status Report'
      @status_report = elastic_agent_plugin_service.getPluginStatusReport(params[:plugin_id])
    rescue java.lang.UnsupportedOperationException => e
      render_error_template "Status Report for plugin with id: #{params[:plugin_id]} is not found.", 404
    rescue java.lang.Exception => e
      render_plugin_error e
    end

    def agent_status
      @view_title = 'Agent Status Report'
      @page_header = 'Agent Status Report'

      unless valid_params?
        return render_error_template 'Please provide job_id for Agent Status Report.', 422
      end

      @agent_status_report = elastic_agent_plugin_service.getAgentStatusReport(params[:plugin_id], job_identifier, elastic_agent_id)
    rescue org.springframework.dao.DataRetrievalFailureException, java.lang.UnsupportedOperationException
      render_error_template "Status Report for plugin with id: #{params[:plugin_id]} for agent #{params[:elastic_agent_id]} is not found.", 404
    rescue java.lang.Exception => e
      render_plugin_error e
    end

    def cluster_status
      @view_title = 'Cluster Status Report'
      @page_header = 'Cluster Status Report'
      @cluster_status_report = elastic_agent_plugin_service.getClusterStatusReport(params[:plugin_id], params[:cluster_profile_id])
    rescue org.springframework.dao.DataRetrievalFailureException, java.lang.UnsupportedOperationException
      render_error_template "Status Report for plugin with id: #{params[:plugin_id]} for cluster #{params[:cluster_profile_id]} is not found.", 404
    rescue java.lang.Exception => e
      render_plugin_error e
    end

    private

    def elastic_agent_id
      (params[:elastic_agent_id].eql? 'unassigned') ? nil : params[:elastic_agent_id]
    end

    def job_identifier
      unless params[:job_id].blank?
        job_instance = job_instance_service.buildById params[:job_id].to_i
        job_instance.getIdentifier
      end
    end

    def valid_params?
      !params[:job_id].blank?
    end

    def load_plugin_info
      plugin_info = ElasticAgentMetadataStore.instance().getPluginInfo(params[:plugin_id])
      render_error_template "Plugin with id: #{params[:plugin_id]} is not found.", 404 if plugin_info.nil?
    end

    def render_plugin_error e
      message = e.cause != nil ? e.cause.message : e.message
      self.error_template_for_request = 'status_report_error'
      render_error_template message, 500
    end
  end
end
