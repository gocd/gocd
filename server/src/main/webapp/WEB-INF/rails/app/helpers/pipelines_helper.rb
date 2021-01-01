#
# Copyright 2021 ThoughtWorks, Inc.
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

module PipelinesHelper
  include JavaImports
  include StagesHelper
  include ERB::Util

  def stage_bar_url stage, action
    stage_id = stage.getIdentifier()
    stage.isScheduled() ? stage_detail_tab_path_for(:stage_name => stage_id.getStageName(), :stage_counter => stage_id.getStageCounter(), :pipeline_name => stage_id.getPipelineName(), :pipeline_counter => stage_id.getPipelineCounter(), :action => action) : "#"
  end

  def run_stage_label stage
    (stage.isScheduled() ? "rerun" : "trigger")
  end

  def trigger_message(actual_date_time, pim)
    if pim.class != com.thoughtworks.go.presentation.pipelinehistory.PreparingToScheduleInstance
      "Triggered&nbsp;#{'by'}&nbsp;<span class='who'>#{h pim.getApprovedBy()}</span>"
    else
      ""
    end
  end

  def trigger_message_with_formatted_date_time(date_time, who)
    on = "&nbsp;on&nbsp;<span class='time' data='#{date_time.getTime()}' title='#{date_time.to_long_display_date_time} server time'></span>"
    if who == GoConstants::DEFAULT_APPROVED_BY
      "<span class='label'>Automatically triggered</span>#{on}".html_safe
    else
      "<span class='label'>Triggered</span>&nbsp;by&nbsp;<span class='who'>#{h who}</span>#{on}".html_safe
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

  def url_for_pipeline_value_stream_map(pipeline, options = {})
    vsm_show_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :action => "show"}))
  end

  def url_for_dmr(dmr)
    "/go/pipelines/value_stream_map/#{dmr.getPipelineName()}/#{dmr.getPipelineCounter()}"
  end

  def with_pipeline_analytics_support(&block)
    return unless block_given?

    return if show_analytics_only_for_admins? && !is_user_an_admin?

    default_plugin_info_finder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION).each do |combined_plugin_info|
      extension_info = combined_plugin_info.extensionFor(PluginConstants.ANALYTICS_EXTENSION)
      if extension_info.getCapabilities().supportsPipelineAnalytics()
        supported_analytics = extension_info.getCapabilities().supportedPipelineAnalytics().get(0)
        yield combined_plugin_info.getDescriptor().id(), supported_analytics.getId()
        break
      end
    end
  end

  def show_analytics_only_for_admins?
    system_environment.enableAnalyticsOnlyForAdmins
  end
end
