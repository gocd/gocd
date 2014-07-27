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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe StageHistoryAPIModel do
  include APIModelMother

  before(:each) do
    @pagination_view_model = create_pagination_model
    @stage_view_model = [create_stage_model]
  end

  describe "should initialize correctly" do
    it "should populate correct data" do
      stage_history_api_model = StageHistoryAPIModel.new(@pagination_view_model, @stage_view_model)

      stage_history_api_model.pagination.page_size.should == 10
      stage_history_api_model.pagination.offset.should == 1
      stage_history_api_model.pagination.total.should == 100

      stage_instance_api_model = stage_history_api_model.stages[0]
      stage_instance_api_model.id.should == 4
      stage_instance_api_model.name.should == 'stage name'
      stage_instance_api_model.counter.should == '1'
      stage_instance_api_model.scheduled.should == false
      stage_instance_api_model.approval_type.should == 'manual'
      stage_instance_api_model.approved_by.should == 'me'
      stage_instance_api_model.result.should == 'passed'
      stage_instance_api_model.rerun_of_counter.should == 1
      stage_instance_api_model.operate_permission.should == 'yes'
      stage_instance_api_model.can_run.should == true
      stage_instance_api_model.pipeline_name.should == 'pipeline'
      stage_instance_api_model.pipeline_counter.should == 1
    end
  end
end