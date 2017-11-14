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

module StagesHelper

  def placeholder_stage? stage
    stage.getState() == com.thoughtworks.go.domain.StageState::Unknown
  end

  def is_current_stage?(identifier)
    params[:pipeline_name] == identifier.getPipelineName() &&
      params[:pipeline_counter].to_i == identifier.getPipelineCounter().to_i &&
      params[:stage_name] == identifier.getStageName() &&
      params[:stage_counter].to_i == identifier.getStageCounter().to_i
  end

  def stage_detail_path_for_identifier(identifier, options = {})
    stage_detail_tab_default_path(options.merge(:pipeline_name => identifier.getPipelineName(),
                                                :pipeline_counter => identifier.getPipelineCounter(),
                                                :stage_name => identifier.getStageName(),
                                                :stage_counter => identifier.getStageCounter()))
  end

  def stage_detail_pipeline_tab_for_identifier identifier
    tab_aware_path_for_stage identifier, 'pipeline'
  end

  def tab_aware_path_for_stage stage_identifier, tab
    stage_detail_tab_path_for({:pipeline_name => stage_identifier.getPipelineName(),
                               :pipeline_counter => stage_identifier.getPipelineCounter(),
                               :stage_name => stage_identifier.getStageName(),
                               :stage_counter => stage_identifier.getStageCounter()}, tab)
  end

  def stage_detail_tab_path_for options, tab
    tab = tab || 'default'
    if options.nil?
      send("stage_detail_tab_#{tab}_path")
    else
      send("stage_detail_tab_#{tab}_path", options)
    end
  end

  def empty_stage(stage_instance_model)
    stage_instance_model.instance_of? com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem
  end

  def link_with_current_tab(link_name, action)
    class_name = action == params[:action] ? ' class="current"' : ''
    "<li#{class_name}>#{link_to(link_name, stage_detail_tab_path_for(nil, action))}</li>".html_safe
  end

  def stage_bar_options sim
    raw tag.tag_options(:class => "stage_bar #{sim.getState()}", :title => "%s (%s)" % [sim.getName(), l.messageFor(sim.getState())])
  end

  def stage_history_pagination_handler page, tab
    dom_id = "stage_history_#{page.getLabel()}"
    url = stage_history_path(:page => page_num=page.getNumber(), :tab => tab)
    <<END
    <a href="#" id="#{dom_id}">#{page.getLabel()}</a>
    <script type="text/javascript">
        StageHistory.bindHistoryLink('##{dom_id}', '#{url}', #{page.getNumber()});
    </script>
END
  end

  def is_config_used_to_run_this_stage_out_of_sync_with_current?(current_config_version, stage_config_version)
    current_config_version != stage_config_version
  end
end