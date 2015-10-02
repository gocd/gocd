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

describe PipelineStatusAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pipeline_status_view_model = create_pipeline_status_model
      pipeline_status_api_model = PipelineStatusAPIModel.new(@pipeline_status_view_model)

      pipeline_status_api_model.paused.should == true
      pipeline_status_api_model.pausedCause.should == 'Pausing it for some reason'
      pipeline_status_api_model.pausedBy.should == 'admin'
      pipeline_status_api_model.locked.should == true
      pipeline_status_api_model.schedulable.should == true
    end
  end
end
