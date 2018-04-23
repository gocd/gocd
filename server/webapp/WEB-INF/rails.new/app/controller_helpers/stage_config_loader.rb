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

module StageConfigLoader
  include JavaImports

  def self.included base
    base.send(:include, ::PipelineConfigLoader)
    base.send(:extend, ClassMethods)
  end

  module ClassMethods
    def load_stage_except_for *actions
      load_pipeline_except_for *actions
      before_action :load_stage, :except => actions
    end
  end

  private

  def load_stage
    stage_name = CaseInsensitiveString.new(params[:stage_name])
    stage = @pipeline.find { |stage_config| stage_name == stage_config.name() }
    assert_load :stage, stage, l.stageNotFoundInPipeline(stage_name, @pipeline.name())
  end
end