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

module ConfigUpdate
  module LoadConfig
    include JavaImports
    
    def load_pipeline_group(cruise_config)
      if (!CaseInsensitiveString.isBlank(pipeline_name))
        cruise_config.getGroups().findGroupNameByPipeline(pipeline_name)
      else
        group = load_pipeline_group_config(cruise_config)
        raise "Unable to find group: #{pipeline_group_name}" if group.nil?
        group.getGroup()
      end
    end

    def load_pipeline_group_config(cruise_config)
      load_all_pipeline_groups(cruise_config).findGroup(pipeline_group_name) if load_all_pipeline_groups(cruise_config).hasGroup(pipeline_group_name)
    end

    def load_all_pipeline_groups(cruise_config)
      cruise_config.getGroups()
    end

    def load_pipeline_group_config_for_pipeline(cruise_config)
      cruise_config.findGroup(load_pipeline_group(cruise_config))
    end

    def load_pipeline(cruise_config)
      cruise_config.pipelineConfigByName(pipeline_name)
    rescue
      nil
    end

    def load_template(cruise_config)
      cruise_config.findTemplate(pipeline_name)
    end

    def looking_at_template?
      params[:stage_parent] == "templates"
    end

    def load_pipeline_or_template(cruise_config)
      if looking_at_template?
        load_template(cruise_config)
      else #assume pipeline
        load_pipeline(cruise_config)
      end
    end

    def load_stage(cruise_config)
      load_stage_from_pipeline(load_pipeline_or_template(cruise_config))
    end

    def load_stage_from_pipeline(pipeline)
      load_from_pipeline_stage_named(pipeline, stage_name)
    end

    def load_from_pipeline_stage_named(pipeline, stage_name)
      pipeline && pipeline.getStage(stage_name)
    end

    def load_material_config_for_pipeline(cruise_config)
      finger_print = params[:finger_print]
      pipeline = load_pipeline(cruise_config)
      pipeline && pipeline.materialConfigs().getByFingerPrint(finger_print)
    end

    def load_job(cruise_config)
      stage = load_stage(cruise_config)
      load_job_from_stage_named(stage, job_name)
    end

    def load_job_from_stage_named(stage, job_name)
      stage && stage.jobConfigByConfigName(job_name)
    end

    def load_task(job, index)
      job && (index < job.getTasks().size()) ? job.getTasks().get(index) : nil
    end

    def load_task_of_job(cruise_config, index)
      load_task(load_job(cruise_config), index)
    end

    def task_index
      params[:task_index].to_i
    end

    def pipeline_group_name
      params[:group_name]
    end

    def pipeline_name
      CaseInsensitiveString.new(params[:pipeline_name])
    end

    def stage_name
      CaseInsensitiveString.new(params[:stage_name])
    end

    def job_name
      CaseInsensitiveString.new(params[:job_name])
    end

    def template_name
      params[:pipeline_name]
    end

    def self.for params
      Class.new() do
        attr_reader :params
        def initialize params
          @params = params
        end
        include ::ConfigUpdate::LoadConfig
      end.new(params)
    end
  end
end