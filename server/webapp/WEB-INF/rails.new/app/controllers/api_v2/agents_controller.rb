##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

module ApiV2
  class AgentsController < ApiV2::BaseController

    before_action :check_user_and_404
    before_action :check_admin_user_and_401, except: [:index, :show]

    before_action :load_agent, only: [:show, :edit, :update, :destroy, :enable, :disable]

    def index
      presenters = AgentsRepresenter.new(agent_service.agents.to_a)
      render DEFAULT_FORMAT => presenters.to_hash(url_builder: self)
    end

    def show
      render DEFAULT_FORMAT => agent_presenter.to_hash(url_builder: self)
    end

    def update
      result    = HttpOperationResult.new

      @agent_instance = agent_service.updateAgentAttributes(current_user, result, params[:uuid], params[:hostname], maybe_join(params[:resources]), maybe_join(params[:environments]), to_enabled_tristate)

      if result.isSuccess
        load_agent
        render DEFAULT_FORMAT => agent_presenter.to_hash(url_builder: self)
      else
        json = agent_presenter.to_hash(url_builder: self)
        render_http_operation_result(result, {data: json})
      end
    end

    def maybe_join(obj)
      if obj.is_a?(Array)
        obj.join(',')
      else
        obj
      end
    end

    def destroy
      result = HttpOperationResult.new
      agent_service.deleteAgents(current_user, result, [params[:uuid]])
      render_http_operation_result(result)
    end

    private

    attr_reader :agent_instance

    def to_enabled_tristate
      enabled = params[:agent_config_state]
      if enabled.blank?
        TriState.UNSET
      elsif enabled =~ /enabled/i
        TriState.TRUE
      elsif enabled =~ /disabled/i
        TriState.FALSE
      else
        raise BadRequest.new('The value of `agent_config_state` can be one of `Enabled`, `Disabled` or null.')
      end
    end

    def load_agent
      @agent_instance = agent_service.findAgent(params[:uuid])
      raise RecordNotFound if @agent_instance.nil? || @agent_instance.isNullAgent()
    end

    def agent_presenter
      AgentRepresenter.new(agent_view_model)
    end

    def agent_view_model
      com.thoughtworks.go.server.ui.AgentViewModel.new(@agent_instance, environment_config_service.environmentsFor(@agent_instance.getUuid())) if @agent_instance
    end
  end

end
