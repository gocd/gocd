##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

class EnvironmentsController < ApplicationController
  before_filter :load_new_environment, :only => [:new, :create]
  before_filter :load_existing_environment, :only => [:edit, :update, :show, :edit_pipelines, :edit_agents, :edit_variables]

  before_filter :load_pipelines_and_agents, :only => [:new, :edit, :create, :update, :edit_pipelines, :edit_agents]

  prepend_before_filter :default_as_empty_list, :only => [:update]
  skip_before_filter :verify_authenticity_token

  layout "application", :except => [:edit_pipelines, :edit_agents, :edit_variables]
  prepend_before_filter :set_tab_name

  def index
    @environments = environment_service.getEnvironments(current_user)
    @show_add_environments = security_service.isUserAdmin(current_user)
  end

  def new
  end

  def create
    @environment.setConfigAttributes(params[:environment])
    if @environment.name().blank?
      render_environment_create_error_with_message(@environment, l.string("ENVIRONMENT_NAME_REQUIRED"), 400)
      return
    end

    environment_config_service.createEnvironment(@environment, current_user, @result = HttpLocalizedOperationResult.new)
    render_environment_create_error_result(@environment)
    redirect_with_flash(l.string("ADD_ENVIRONMENT_SUCCESS", [@environment.name()]), :action => :show, :name => @environment.name().to_s, :class => 'success') if @result.isSuccessful()
  end

  def update
    @environment.setConfigAttributes(params[:environment])
    if @environment.name().blank?
      render_error_response l.string("ENVIRONMENT_NAME_REQUIRED"), 400, true
      return
    end

    old_environment = environment_config_service.getEnvironmentConfig(params[:name])
    result = HttpLocalizedOperationResult.new
    environment_config_service.updateEnvironment(old_environment, @environment, current_user, params[:cruise_config_md5], result)

    message = result.message(Spring.bean('localizer'))
    if result.isSuccessful()
      render :text => message, :location => url_options_with_flash(message, {:action => :show, :name => @environment.name(), :class => 'success', :only_path => true})
    else
      render_error_response message, 400, true
    end
  end

  def show
    @agent_details = agent_service.filter(@environment.getLocalAgents().map(&:uuid))
  end

  def edit_pipelines
    render layout:false
  end

  def edit_agents
    render layout:false
  end

  def edit_variables
    render layout:false
  end

  private

  def load_new_environment
    @environment = BasicEnvironmentConfig.new
  end

  def load_existing_environment
    result = HttpLocalizedOperationResult.new
    env_for_edit = environment_config_service.forEdit(params[:name], result)
    if (result.isSuccessful())
      @environment = env_for_edit.getConfigElement()
      @cruise_config_md5 = entity_hashing_service.md5ForEntity(@environment)
    end
    render_if_error(result.message(Spring.bean('localizer')), result.httpCode())
    result.isSuccessful()
  end

  def render_environment_create_error_result(environment)
    render_environment_create_error_with_message(environment, @result.message(localizer), @result.httpCode()) unless @result.isSuccessful()
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
    @remote_pipelines = environment_config_service.getAllRemotePipelinesForUserInEnvironment(current_user,@environment)

    pipelines.each do |pipeline|
      collection = pipeline.isAssociatedWithEnvironmentOtherThan(@environment && @environment.name().to_s) ? @unavailable_pipelines : @available_pipelines
      collection << pipeline
    end

    @agents = agent_service.registeredAgents()
    @agents.sortBy(AgentViewModel.HOSTNAME_COMPARATOR, SortOrder::ASC)
  end

    def set_tab_name
       @current_tab_name = "environments"
    end
end
