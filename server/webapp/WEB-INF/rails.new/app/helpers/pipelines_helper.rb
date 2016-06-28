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
  include ERB::Util

  def stage_bar_url stage, action
    stage_id = stage.getIdentifier()
    stage.isScheduled() ? stage_detail_tab_path(:stage_name => stage_id.getStageName(), :stage_counter => stage_id.getStageCounter(), :pipeline_name => stage_id.getPipelineName(), :pipeline_counter => stage_id.getPipelineCounter(), :action => action) : "#"
  end

  def run_stage_label stage
    (stage.isScheduled() ? "rerun" : "trigger")
  end

  def trigger_message(actual_date_time, pim)
    if pim.class != com.thoughtworks.go.presentation.pipelinehistory.PreparingToScheduleInstance
      "(Triggered&nbsp;#{l.string('BY')}&nbsp;<span class='who'>#{h pim.getApprovedBy()}</span>&nbsp;<span class='time'></span><input type='hidden' value='#{actual_date_time}'/>)"
    else
    ""
    end
  end

  def trigger_message_with_formatted_date_time(date_time, who)
    on = "&nbsp;#{l.string("on")}&nbsp;<span class='time'>#{date_time.to_long_display_date_time}</span>"
    if who == GoConstants::DEFAULT_APPROVED_BY
      "<span class='label'>#{l.string("AUTO_TRIGGERED")}</span>#{on}".html_safe
    else
      "<span class='label'>#{l.string("Triggered")}</span>&nbsp;#{l.string("by")}&nbsp;<span class='who'>#{h who}</span>#{on}".html_safe
    end
  end

  def pipelines_pipeline_dom_id(pipeline_model)
    "pipeline_#{pipeline_name(pipeline_model)}_panel"
  end

  def pipelines_dom_id(pipeline_group_name)
    "pipeline_group_#{pipeline_group_name}_panel"
  end

  def pipeline_name pipeline_model
    pipeline_model.getLatestPipelineInstance().getName()
  end

  def material_type(material)
    dependency?(material) ? 'dependency' : 'scm'
  end

  def dependency?(material)
    material.is_a? DependencyMaterial
  end

  def revision_for(revision)
    if dependency?(revision.getMaterial())
      "#{revision.getRevision().getPipelineName()}/#{revision.getRevision().getPipelineCounter()}"
    else
      revision.getLatestShortRevision()
    end
  end

  def pipeline_instance_identifier(pim)
    "#{pim.getName()}_#{pim.getCounter()}"
  end

  def pipeline_build_cause_popup_id(pim)
    "changes_#{pipeline_instance_identifier(pim)}"
  end

  def build_cause_popup_id_for_pipeline_counter(counter, prefix = nil)
    "#{prefix}for_pipeline_#{counter}"
  end

  def url_for_pipeline_instance(pipeline, options = {})
    stage = pipeline.latestStage()
    stage_detail_tab_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :stage_name => stage.getName(), :stage_counter => stage.getCounter(), :action => "pipeline"}))
  end

  def url_for_pipeline_value_stream_map(pipeline, options = {})
    vsm_show_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :action => "show"}))
  end

  def url_for_dmr(dmr)
    stage_detail_tab_path({:pipeline_name => dmr.getPipelineName(), :pipeline_counter => dmr.getPipelineCounter(), :stage_name => dmr.getStageName(), :stage_counter => dmr.getStageCounter(), :action => "pipeline"})
  end
end