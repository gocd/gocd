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

module PipelinesHelper
  include RailsLocalizer
  include JavaImports

  # TODO: SBD: As a part of new Rails migration, methods in this file need to be uncommented and tested.

  #def stage_status_for_ui state
  #  is_stage_unknown = state.stageResult() == StageResult::Unknown
  #  stage_result_to_string = state.stageResult().toString()
  #  l.string(is_stage_unknown ? "Building" : stage_result_to_string)
  #end
  #
  #def stage_bar_url stage, action
  #  stage_id = stage.getIdentifier()
  #  stage.isScheduled() ? stage_detail_tab_path(:stage_name => stage_id.getStageName(), :stage_counter => stage_id.getStageCounter(), :pipeline_name => stage_id.getPipelineName(), :pipeline_counter => stage_id.getPipelineCounter(), :action => action) : "#"
  #end
  #
  #def run_stage_label stage
  #  (stage.isScheduled() ? "rerun" : "trigger")
  #end
  #
  #def trigger_message(actual_date_time, pim)
  #  if pim.class != com.thoughtworks.go.presentation.pipelinehistory.PreparingToScheduleInstance
  #    "(Triggered&nbsp;#{l.string('BY')}&nbsp;<span class='who'>#{pim.getApprovedBy()}</span>&nbsp;<span class='time'></span><input type='hidden' value='#{actual_date_time}'/>)"
  #  else
  #  ""
  #  end
  #
  #end
  #
  #def trigger_message2(actual_date_time, pim)
  #  if pim.class != com.thoughtworks.go.presentation.pipelinehistory.PreparingToScheduleInstance
  #    "<span class='time'></span><input type='hidden' value='#{actual_date_time}'/>&nbsp;#{l.string('BY')}&nbsp;<span class='who'>#{pim.getApprovedBy()}</span>"
  #  end
  #end
  #
  #def trigger_message_with_formatted_date_time(date_time, who)
  #  on = "&nbsp;#{l.string("on")}&nbsp;<span class='time'>#{date_time.to_long_display_date_time}</span>"
  #  if who == GoConstants::DEFAULT_APPROVED_BY
  #    "<span class='label'>#{l.string("AUTO_TRIGGERED")}</span>#{on}"
  #  else
  #    "<span class='label'>#{l.string("Triggered")}</span>&nbsp;#{l.string("by")}&nbsp;<span class='who'>#{who}</span>#{on}"
  #  end
  #end
  #
  #def clear_after(idx)
  #  idx % 3 == 2 ? " clear_after" : ""
  #end
  #
  #def pipelines_pipeline_dom_id(pipeline_model)
  #  "pipeline_#{pipeline_name(pipeline_model)}_panel"
  #end
  #
  #def pipelines_dom_id(pipeline_group_name)
  #  "pipeline_group_#{pipeline_group_name}_panel"
  #end
  #
  #def material_type(material)
  #  dependency?(material) ? 'dependency' : 'scm'
  #end
  #
  #def scm?(material)
  #  material.is_a? ScmMaterial
  #end
  #
  #def dependency?(material)
  #  material.is_a? DependencyMaterial
  #end
  #
  #def revision_for(revision)
  #  if dependency?(revision.getMaterial())
  #    "#{revision.getRevision().getPipelineName()}/#{revision.getRevision().getPipelineCounter()}"
  #  else
  #    revision.getLatestShortRevision()
  #  end
  #end
  #
  def pipeline_instance_identifier(pim)
    "#{pim.getName()}_#{pim.getCounter()}"
  end
  #
  #def pipeline_build_cause_popup_id(pim)
  #  "changes_#{pipeline_instance_identifier(pim)}"
  #end
  #
  #def build_cause_popup_id_for_pipeline_counter(counter, prefix = nil)
  #  "#{prefix}for_pipeline_#{counter}"
  #end
  #
  #def url_for_pipeline_instance(pipeline, options = {})
  #  stage = pipeline.latestStage()
  #  stage_detail_tab_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :stage_name => stage.getName(), :stage_counter => stage.getCounter(), :action => "pipeline"}))
  #end
  #
  #def url_for_pipeline_value_stream_map(pipeline, options = {})
  #  vsm_show_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :action => "show"}))
  #end
  #
  def url_for_dmr(dmr)
    stage_detail_tab_path({:pipeline_name => dmr.getPipelineName(), :pipeline_counter => dmr.getPipelineCounter(), :stage_name => dmr.getStageName(), :stage_counter => dmr.getStageCounter(), :action => "pipeline"})
  end
  #
  #def url_for_remote_pipeline(pipeline_name, remote_server)
  #  "#{remote_server.getHostConfiguration().getHostURL()}/go/tab/pipeline/history/#{pipeline_name}"
  #end
  #
  #def url_for_dmr_for_remote_pipeline(dmr, remote_server)
  #  options = {:pipeline_name => dmr.getPipelineName(), :pipeline_counter => dmr.getPipelineCounter(), :stage_name => dmr.getStageName(), :stage_counter => dmr.getStageCounter(), :action => "pipeline"}
  #  if remote_server
  #    port = remote_server.getHostConfiguration().getPort()
  #    host = "#{remote_server.getHostConfiguration().getHost()}#{port == 80 ? '' : ':' + port.to_s}"
  #    options.merge!({:only_path => false, :host => host})
  #  end
  #  stage_detail_tab_url(options)
  #end
end