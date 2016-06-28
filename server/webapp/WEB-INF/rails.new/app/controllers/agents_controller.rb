##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

class AgentsController < ApplicationController
  include AgentBulkEditor

  AgentViewModel = com.thoughtworks.go.server.ui.AgentViewModel
  AgentsViewModel = com.thoughtworks.go.server.ui.AgentsViewModel

  ORDERS = HashWithIndifferentAccess.new({
    :ip_address => AgentViewModel.IP_ADDRESS_COMPARATOR,
    :status => AgentViewModel.STATUS_COMPARATOR,
    :hostname => AgentViewModel.HOSTNAME_COMPARATOR,
    :usable_space => AgentViewModel.USABLE_SPACE_COMPARATOR,
    :location => AgentViewModel.LOCATION_COMPARATOR,
    :resources => AgentViewModel.RESOURCES_COMPARATOR,
    :environments => AgentViewModel.ENVIRONMENTS_COMPARATOR,
    :operating_system => AgentViewModel.OS_COMPARATOR,
    :bootstrapper_version => AgentViewModel.BOOTSTRAPPER_VERSION_COMPARATOR
  })

  LISTING_MESSAGE_KEY = :agents_flash_message
  before_filter :apply_default_sort_unless_sorted, :only => :index

  layout "application"
  prepend_before_filter :set_tab_name

  def index
    @agents = agent_service.agents
    @agents.filter(filter)
    @agents.sortBy(ORDERS[params[:column]], specified_order)

    @agents_disabled = @agents.disabledCount()
    @agents_pending = @agents.pendingCount()
    @agents_enabled = @agents.enabledCount()

    @flash_message = LISTING_MESSAGE_KEY
  end

  def resource_selector
    @selections = agent_service.getResourceSelections(params[:selected] || [])
    render :layout => false
  end

  def environment_selector
    @selections = agent_service.getEnvironmentSelections(params[:selected] || [])
    render :layout => false
  end

  def edit_agents
    result = bulk_edit
    session[LISTING_MESSAGE_KEY] = FlashMessageModel.new(result.message(), result.canContinue() ? 'success' : 'error')
    redirect_to action: "index", filter: params[:filter], order: params[:order], column: params[:column]
  end

  private

  def filter
    CGI.unescapeHTML(params[:filter]) if params[:filter]
  end

  def populate_agent_for_details
    uuid = params[:uuid]
    @agent = agent_service.findAgentViewModel(uuid)
    if @agent.isNullAgent()
      render_error_response(l.string("AGENT_WITH_UUID_NOT_FOUND", [uuid].to_java(java.lang.String)), 404, false)
      false
    end
  end

  def apply_default_sort_unless_sorted
    params[:column] ||= "status"
    params[:order] ||= "ASC"
  end

  def specified_order default="ASC"
    com.thoughtworks.go.server.ui.SortOrder.orderFor(params[:order] || default)
  end


  def set_tab_name
    @current_tab_name = "agents"
  end
end
