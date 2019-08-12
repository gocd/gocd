#
# Copyright 2019 ThoughtWorks, Inc.
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
#

class EnvironmentsController < ApplicationController
  helper FlashMessagesHelper
  helper SortableTableHelper
  helper AgentsHelper

  before_action :load_new_environment, :only => [:new, :create]
  before_action :load_existing_environment, :only => [:show, :update, :edit_pipelines, :edit_agents, :edit_variables]

  before_action :load_pipelines_and_agents, :only => [:new, :create, :update, :edit_pipelines, :edit_agents]

  before_action :load_pipelines_and_agents, :only => [:new, :edit, :create, :update, :edit_pipelines, :edit_agents]

  prepend_before_action :default_as_empty_list, :only => [:update]
  skip_before_action :verify_authenticity_token

  layout "application", :except => [:edit_pipelines, :edit_agents, :edit_variables]
  prepend_before_action :set_tab_name

  def index
    @environments = environment_config_service.listAllMergedEnvironments()
    set_agent_details
    @show_add_environments = security_service.isUserAdmin(current_user)
  end

  def new
  end

  def show
    @agent_details = agent_service.filter(@environment.getAgents().map(&:uuid))
  end

  def create
    original_env_config = @environment;
    @environment.setConfigAttributes(params[:environment])
    if @environment.name().blank?
      render_environment_create_error_with_message(@environment, 'Environment name is required', 400)
      return
    end

    env_agents_config = @environment.getAgents()
    selected_uuids = env_agents_config.getUuids()

    result1 = HttpLocalizedOperationResult.new
    result2 = HttpLocalizedOperationResult.new

    if !selected_uuids.empty?
        agent_service.updateAgentsAssociationWithSpecifiedEnv(original_env_config, selected_uuids, result1)

        @result = result1
        if result1.isSuccessful()
          # Env were successfully associated with agents in DB. Now creating env in the config
          environment_config_service.createEnvironment(@environment, current_user, result2)
          @result = result2
        end

        if result1.isSuccessful() && !result2.isSuccessful()
          # Error while creating that env in config. So rolling back (deleting environment association from agents) in DB
          result3 = HttpLocalizedOperationResult.new
          agent_service.updateAgentsAssociationWithSpecifiedEnv(original_env_config, [], result3)
        end
    else
        environment_config_service.createEnvironment(@environment, current_user, result1)

        @result = result1
    end

    render_environment_create_error_result(@environment)
    redirect_with_flash("Added environment '#{@environment.name()}'", :action => :index, :class => 'success') if @result.isSuccessful()
  end

  def update
    environment_attributes = params[:environment]
    original_env_config = @environment;
    original_agents = @environment.agents
    environment_config_name = params[:name]
    @environment = environment_config_service.getEnvironmentForEdit(environment_config_name)
    @environment.setConfigAttributes(environment_attributes)
    if @environment.name().blank?
      render_error_response 'Environment name is required', 400, true
      return
    end
    result = HttpLocalizedOperationResult.new
    if environment_attributes.key?(EnvironmentConfig.AGENTS_FIELD)
      finalUUIDsToAssociate = environment_attributes.fetch(EnvironmentConfig.AGENTS_FIELD)
                                .map {|agent_uuid| agent_uuid.fetch('uuid')}

      message = "Updated environment '#{environment_config_name}'."
      if finalUUIDsToAssociate.empty? && original_agents.empty?
        return render :plain => message, :location => url_options_with_flash(message, {:action => :index, :class => 'success', :only_path => true})
      end

      agent_service.updateAgentsAssociationWithSpecifiedEnv(original_env_config, finalUUIDsToAssociate, result)
      if !result.isSuccessful()
        message = result.message()
        return render_error_response message, result.httpCode(), true
      end
    end

    environment_attributes.delete(EnvironmentConfig.AGENTS_FIELD)
    if environment_attributes.key?(EnvironmentConfig.PIPELINES_FIELD) || environment_attributes.key?(EnvironmentConfig.VARIABLES_FIELD)
      environment_config_service.updateEnvironment(environment_config_name, @environment, current_user, params[:cruise_config_md5], result)
      message = result.message()
    end

    if result.isSuccessful()
      render :plain => message, :location => url_options_with_flash(message, {:action => :index, :class => 'success', :only_path => true})
    else
      render_error_response message, 400, true
    end
  end

  def edit_pipelines
    render layout: false
  end

  def edit_agents
    render layout: false
  end

  def edit_variables
    render layout: false
  end

  private

  def set_agent_details
    @environments.each do |env_view_model|
      env_view_model.setAgentViewModels(agent_service)
    end
  end

  def load_new_environment
    @environment = BasicEnvironmentConfig.new
  end

  def load_existing_environment
    result = HttpLocalizedOperationResult.new
    env_for_display = environment_config_service.getMergedEnvironmentforDisplay(params[:name], result)
    if (result.isSuccessful())
      @environment = env_for_display.getConfigElement()
      @cruise_config_md5 = entity_hashing_service.md5ForEntity(environment_config_service.getEnvironmentForEdit(params[:name]))
    end
    render_if_error(result.message(), result.httpCode())
    result.isSuccessful()
  end

  def render_environment_create_error_result(environment)
    render_environment_create_error_with_message(environment, @result.message(), @result.httpCode()) unless @result.isSuccessful()
  end

  def render_environment_create_error_with_message(environment, message, http_code)
    session[:notice] = FlashMessageModel.new(message, "error")
    new
    @environment = environment
    render :action => "new", :status => http_code
  end

  def load_pipelines_and_agents
    pipelines = environment_config_service.getAllLocalPipelinesForUser(current_user)
    # available_pipelines should only contain local pipelines, not referenced already from a remote config repository

    @unavailable_pipelines = []
    @available_pipelines = []
    @remote_pipelines = environment_config_service.getAllRemotePipelinesForUserInEnvironment(current_user, @environment)

    pipelines.each do |pipeline|
      collection = pipeline.isAssociatedWithEnvironmentOtherThan(@environment && @environment.name().to_s) ? @unavailable_pipelines : @available_pipelines
      collection << pipeline
    end

    @agents = agent_service.getRegisteredAgentsViewModel()
    @agents.sortBy(AgentViewModel.HOSTNAME_COMPARATOR, SortOrder::ASC)
  end

  def set_tab_name
    @current_tab_name = "environments"
  end
end
