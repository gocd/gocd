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

module PipelineConfigLoader
  include JavaImports
  include PauseInfoLoader
  
  def self.included base
    base.send(:extend, ClassMethods)
  end

  module ClassMethods
    def load_pipeline_for_all_actions
      load_pipeline_except_for
    end

    def load_pipeline_except_for *except_for
      options = except_for.extract_options!
      before_filter :load_pipeline, :except => except_for
      before_filter(:load_pause_info, :except => except_for) unless options[:skip_pause_info]
    end
  end

  def load_pipeline
    result = HttpLocalizedOperationResult.new
    if (params[:stage_parent] == 'templates')
      pipeline_for_edit = template_config_service.loadForEdit(params[:pipeline_name], current_user, result)
    else
      pipeline_for_edit = go_config_service.loadForEdit(params[:pipeline_name], current_user, result)
    end
    unless result.isSuccessful()
      render_localized_operation_result result
      return
    end
    assert_load(:pipeline, pipeline_for_edit.getConfig()) &&
            assert_load(:cruise_config, pipeline_for_edit.getCruiseConfig()) &&
            assert_load(:processed_cruise_config, pipeline_for_edit.getProcessedConfig())
  end
end