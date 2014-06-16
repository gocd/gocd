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

module JobConfigLoader
  include JavaImports

  def self.included base
    base.send(:include, ::StageConfigLoader)
    base.send(:extend, ClassMethods)
  end

  module ClassMethods
    def load_job_except_for *actions
      load_stage_except_for *actions
      before_filter :load_job, :except => actions
    end
  end

  private

  def load_job
    job_name = CaseInsensitiveString.new(params[:job_name])
    assert_load :job, @stage.getJobs().find { |job_config| job_name == job_config.name() }, l.jobNotFoundInStage(job_name, @stage.name(), @pipeline.name())
  end
end