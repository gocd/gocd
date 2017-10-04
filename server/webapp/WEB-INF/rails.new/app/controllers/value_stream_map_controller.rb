##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

class ValueStreamMapController < ApplicationController
  include ApplicationHelper, PipelinesHelper
  layout "value_stream_map"

  before_filter :redirect_to_stage_pdg_if_ie8, :only => [:show]

  def show
    begin
      @pipeline = pipeline_service.findPipelineByCounterOrLabel(params[:pipeline_name], params[:pipeline_counter])
    rescue
    end
    respond_to do |format|
      format.html
      format.json {render :json => generate_vsm_json}
    end
  end

  def show_material
    respond_to do |format|
      format.html {@material_display_name = material_display_name}
      format.json {render :json => generate_material_vsm_json}
    end
  end

  private

  def material_display_name()
    material_config = material_config_service.getMaterialConfig(current_user.getUsername().toString(), params[:material_fingerprint], HttpOperationResult.new)
    material_config.getDisplayName() unless material_config.nil?
  end

  def generate_vsm_json
    result = HttpLocalizedOperationResult.new
    vsm = value_stream_map_service.getValueStreamMap(params[:pipeline_name], params[:pipeline_counter].to_i, current_user, result)
    render_vsm_json(vsm, result)
  end

  def generate_material_vsm_json
    result = HttpLocalizedOperationResult.new
    vsm = value_stream_map_service.getValueStreamMap(params[:material_fingerprint], params[:revision], current_user, result)
    render_vsm_json(vsm, result)
  end

  def render_vsm_json(vsm, result)
    vsm_path_partial = proc {|name, counter| vsm_show_path(name, counter)}
    vsm_material_path_partial = proc {|material_fingerprint, revision| vsm_show_material_path(material_fingerprint, revision)}
    stage_detail_path_partial = proc do |pipeline_name, pipeline_counter, stage_name, stage_counter|
      stage_detail_tab_path(:pipeline_name => pipeline_name, :pipeline_counter => pipeline_counter, :stage_name => stage_name, :stage_counter => stage_counter)
    end
    pipeline_edit_path_proc = proc {|pipeline_name| edit_path_for_pipeline(pipeline_name)}
    ValueStreamMapModel.new(vsm, result.message(localizer), localizer, vsm_path_partial, vsm_material_path_partial, stage_detail_path_partial, pipeline_edit_path_proc).to_json
  end

  def redirect_to_stage_pdg_if_ie8
    format = params[:format]
    user_agent = request.env["HTTP_USER_AGENT"]
    if (is_ie8?(user_agent) and (format.blank? || format == :html))
      result = HttpOperationResult.new
      pim = pipeline_history_service.findPipelineInstance(params[:pipeline_name], params[:pipeline_counter].to_i, current_user, result)
      redirect_to url_for_pipeline_instance(pim)
    end
  end

end
