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


module EnvironmentsHelper

  def stage_width_em(total_number_of_stages, is_last_running_stage,total_width)
    last_running_width = is_last_running_stage ? 0.25 : 0.0833
    round_to(((total_width / total_number_of_stages) - last_running_width), 4).to_s + "em"
  end

  def stage_width_percent(total_number_of_stages, is_last_running_stage,total_width)
    last_running_width = is_last_running_stage ? 0 : 0
    round_to(((total_width / total_number_of_stages) - last_running_width), 4).to_s + "%"
  end

  def round_to(float, precision)
    (float * 10**precision).round.to_f / 10**precision
  end

  def env_pipeline_dom_id(pipeline_model)
    "environment_pipeline_#{pipeline_name(pipeline_model)}_panel"
  end

  def env_dom_id(environment_name)
    "environment_#{environment_name}_panel"
  end

  def is_last(list, elem)
    return list[list.length - 1] == elem
  end

  def pipeline_name pipeline_model
    pipeline_model.getLatestPipelineInstance().getName()
  end

  def environment_update_form_options
    {:url => environment_update_url, :method => "put",
     :success => "Modalbox.hide(); location.href=Util.header_value(request.getAllHeaders(), 'Location');",
     :failure => "jQuery('#env_form_error_box').html(Util.flash_message(request.responseText));"}
  end
end
