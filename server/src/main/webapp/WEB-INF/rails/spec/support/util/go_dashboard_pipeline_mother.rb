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

require File.expand_path(File.dirname(__FILE__) + '/pipeline_model_mother')

module GoDashboardPipelineMother
  include PipelineModelMother

  def dashboard_pipeline(pipeline_name, group_name = "group1", permissions = Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), timestamp = 1000)
    clock = double('Clock')
    clock.stub(:currentTimeMillis).and_return(timestamp)
    GoDashboardPipeline.new(pipeline_model(pipeline_name, 'pipeline-label'), permissions, group_name, TimeStampBasedCounter.new(clock))
  end
end