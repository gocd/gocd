#
# Copyright 2019 ThoughtWorks, Inc.
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
      before_action :load_pipeline, :except => except_for
      before_action(:load_pause_info, :except => except_for) unless options[:skip_pause_info]
    end
  end

  def load_pipeline
    result = HttpLocalizedOperationResult.new
    if (params[:stage_parent] == 'templates')
      pipeline_for_edit = template_config_service.loadForEdit(params[:pipeline_name], current_user, result)
    else

      unless go_config_service.canEditPipeline(params[:pipeline_name], current_user, result)
        render_localized_operation_result result
        return
      end

      if go_config_service.isPipelineDefinedInConfigRepository(params[:pipeline_name])
        pipeline_for_edit = go_config_service.loadConfigRepoPipeline(params[:pipeline_name], current_user, result)
        @is_config_repo_pipeline = true
      else
        pipeline_for_edit = go_config_service.loadForEdit(params[:pipeline_name], current_user, result)
        @is_config_repo_pipeline = false
      end

      @pipeline_group_name = pipeline_for_edit.getProcessedConfig.findGroupOfPipeline(pipeline_for_edit.config).group
      @pipeline_md5 = entity_hashing_service.md5ForEntity(pipeline_for_edit.config, @pipeline_group_name)
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
