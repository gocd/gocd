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

require 'spec_helper'

describe JobInstanceAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should clear correct data" do
      job_instance_api_model = JobInstanceAPIModel.new(create_job_model)

      job_instance_api_model.pipeline_name.should == 'pipeline'
      job_instance_api_model.pipeline_counter.should == 123
      job_instance_api_model.stage_name.should == 'stage'
      job_instance_api_model.stage_counter.should == '1'

      job_instance_api_model.clear_pipeline_and_stage_details

      job_instance_api_model.pipeline_name.should == nil
      job_instance_api_model.pipeline_counter.should == nil
      job_instance_api_model.stage_name.should == nil
      job_instance_api_model.stage_counter.should == nil
    end
  end
end
