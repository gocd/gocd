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

class PipelinesController < ApplicationController
  include ApplicationHelper
  layout "application", :except => ["show", "material_search", "show_for_trigger"]

  skip_before_action :verify_authenticity_token, only: [:show_for_trigger, :show, :update_comment]

  before_filter :set_tab_name

  def build_cause
    result = HttpOperationResult.new
    @pipeline_instance = pipeline_history_service.findPipelineInstance(params[:pipeline_name], params[:pipeline_counter].to_i, current_user, result)

    if result.canContinue()
      render "build_cause", layout: false
    else
      render_operation_result_if_failure(result)
    end
  end

  def index
    load_pipeline_related_information
    if @pipeline_configs.isEmpty() && security_service.canCreatePipelines(current_user)
      redirect_to url_for_path("/admin/pipeline/new?group=defaultGroup")
    end
  end

  def show
    populate_and_show(false)
  end

  def show_for_trigger
    populate_and_show(true)
  end

  def material_search
    @matched_revisions = material_service.searchRevisions(params[:pipeline_name], params[:fingerprint], params[:search], current_user, result = HttpLocalizedOperationResult.new)
    unless result.isSuccessful()
      render_localized_operation_result(result)
      return
    end
    @material_type = go_config_service.materialForPipelineWithFingerprint(params[:pipeline_name], params[:fingerprint]).getType
    render layout: false
  end

  def select_pipelines
    pipeline_selections_id = go_config_service.persistSelectedPipelines(cookies[:selected_pipelines], current_user_entity_id, ((params[:selector]||{})[:pipeline]||[]), !params[:show_new_pipelines].nil?)
    cookies[:selected_pipelines] = {:value => pipeline_selections_id, :expires => 1.year.from_now.beginning_of_day} if !mycruise_available?
    render :nothing => true
  end

  def update_comment
    result = HttpLocalizedOperationResult.new

    pipeline_history_service.updateComment(params[:pipeline_name], params[:pipeline_counter].to_i, params[:comment], current_user, result)
    if result.isSuccessful()
      render json: { status: 'success' }
    else
      render_localized_operation_result(result)
    end
  end

  private
  def populate_and_show should_show
    pipeline_name = params[:pipeline_name]
    @pipeline = pipeline_history_service.latest(pipeline_name, current_user)
    @variables = go_config_service.variablesFor(pipeline_name)
    render :partial => "pipeline_material_revisions", :locals => {:scope => {:show_on_pipelines => should_show, :pegged_revisions => params["pegged_revisions"]}}
  end

  def set_tab_name
    @current_tab_name = 'pipelines'
  end

  def load_pipeline_related_information
    @pipeline_selections = go_config_service.getSelectedPipelines(cookies[:selected_pipelines], current_user_entity_id)
    @pipeline_groups = pipeline_history_service.allActivePipelineInstances(current_user, @pipeline_selections)
    @pipeline_configs = security_service.viewableGroupsFor(current_user)
  end
end
