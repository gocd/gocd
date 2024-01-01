#
# Copyright 2024 Thoughtworks, Inc.
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

  def trigger_message_with_formatted_date_time(date_time, who)
    on = "&nbsp;on&nbsp;<span class='time' data='#{date_time.getTime()}' title='#{date_time.to_long_display_date_time} server time'></span>"
    if who == GoConstants::DEFAULT_APPROVED_BY
      "<span class='label'>Automatically triggered</span>#{on}".html_safe
    else
      "<span class='label'>Triggered</span>&nbsp;by&nbsp;<span class='who'>#{html_escape(who)}</span>#{on}".html_safe
    end
  end

  def pipeline_name pipeline_model
    pipeline_model.getLatestPipelineInstance().getName()
  end

  def url_for_pipeline_value_stream_map(pipeline, options = {})
    vsm_show_path(options.merge({:pipeline_name => pipeline.getName(), :pipeline_counter => pipeline.getCounter(), :action => "show"}))
  end

  def url_for_dmr(dmr)
    "/go/pipelines/value_stream_map/#{dmr.getPipelineName()}/#{dmr.getPipelineCounter()}"
  end

  def show_analytics_only_for_admins?
    system_environment.enableAnalyticsOnlyForAdmins
  end
end
